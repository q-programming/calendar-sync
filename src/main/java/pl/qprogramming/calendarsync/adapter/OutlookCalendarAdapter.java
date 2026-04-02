package pl.qprogramming.calendarsync.adapter;

import com.pff.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.port.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.*;
import java.util.*;

@Slf4j
@Component
public class OutlookCalendarAdapter implements OutlookCalendarPort {

    // MS-OXOCAL: minutes from 1601-01-01T00:00Z to Unix epoch 1970-01-01T00:00Z
    private static final long MINUTES_1601_TO_EPOCH = 194074560L;

    @Override
    public List<CalendarRef> listCalendars(String profilePath) {
        validateProfilePath(profilePath);
        List<CalendarRef> result = new ArrayList<>();
        try {
            PSTFile pstFile = new PSTFile(profilePath);
            collectCalendarFolders(pstFile.getRootFolder(), result);
            if (result.isEmpty()) log.warn("No calendar folders found in PST/OST at {}", profilePath);
        } catch (IOException | PSTException e) {
            log.error("Failed to read PST/OST file: {}", profilePath, e);
            throw new RuntimeException("Failed to read Outlook file: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<OutlookEvent> readEvents(String profilePath, String calendarId, DateRange range) {
        validateProfilePath(profilePath);
        List<OutlookEvent> events = new ArrayList<>();
        try {
            log.info("Opening Outlook file: {}", profilePath);
            PSTFile pstFile = new PSTFile(profilePath);
            log.info("Searching for calendar folder '{}'", calendarId);
            PSTFolder calendarFolder = findFolderById(pstFile.getRootFolder(), calendarId);
            if (calendarFolder == null) {
                log.warn("Calendar folder '{}' not found in {}", calendarId, profilePath);
                return List.of();
            }
            log.info("Found folder '{}' ({} items) — reading appointments in range [{}, {}]",
                    calendarFolder.getDisplayName(), calendarFolder.getContentCount(),
                    range.from(), range.to());
            readAppointmentsFromFolder(calendarFolder, range, events);
            log.info("Read {} Outlook events from calendar '{}' in range [{}, {}]",
                    events.size(), calendarId, range.from(), range.to());
        } catch (IOException | PSTException e) {
            log.error("Failed to read Outlook events from {}", profilePath, e);
            throw new RuntimeException("Failed to read Outlook events: " + e.getMessage(), e);
        }
        return events;
    }

    // ── Folder helpers ────────────────────────────────────────────────────────────

    private void collectCalendarFolders(PSTFolder folder, List<CalendarRef> result) {
        try {
            String name = folder.getDisplayName();
            if ("IPF.Appointment".equalsIgnoreCase(folder.getContainerClass())
                    || (name != null && name.equalsIgnoreCase("Calendar"))) {
                result.add(new CalendarRef(String.valueOf(folder.getDescriptorNodeId()),
                        name != null ? name : "Calendar", "UTC", null));
            }
            if (folder.hasSubfolders()) {
                for (PSTFolder sub : folder.getSubFolders()) collectCalendarFolders(sub, result);
            }
        } catch (Exception e) {
            log.debug("Skipping folder during calendar discovery: {}", e.getMessage());
        }
    }

    private PSTFolder findFolderById(PSTFolder root, String id) {
        try {
            if (String.valueOf(root.getDescriptorNodeId()).equals(id)) return root;
            if (root.hasSubfolders()) {
                for (PSTFolder sub : root.getSubFolders()) {
                    PSTFolder found = findFolderById(sub, id);
                    if (found != null) return found;
                }
            }
        } catch (Exception e) {
            log.debug("Error traversing folder tree: {}", e.getMessage());
        }
        return null;
    }

    // ── Appointment reading ────────────────────────────────────────────────────────

    private void readAppointmentsFromFolder(PSTFolder folder, DateRange range, List<OutlookEvent> result) {
        if (folder.getContentCount() <= 0) return;
        int total = folder.getContentCount();
        int count = 0, skipped = 0;

        try {
            PSTMessage item = (PSTMessage) folder.getNextChild();
            while (item != null) {
                count++;
                try {
                    if (item instanceof PSTAppointment appt) {
                        if (appt.isRecurring()) {
                            expandRecurring(appt, range, result);
                        } else {
                            if (quickInRange(appt.getStartTime(), appt.getEndTime(), range)) {
                                OutlookEvent event = toOutlookEvent(appt);
                                if (event != null) result.add(event);
                            } else {
                                skipped++;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.debug("Skipping item {} in '{}': {}", count, folder.getDisplayName(), e.getMessage());
                }

                if (count % 500 == 0) {
                    log.info("  Progress: {}/{} items read, {} matched range so far", count, total, result.size());
                }

                // Advance cursor — skip corrupt nodes
                item = null;
                int skipAttempts = 0;
                while (skipAttempts <= 100) {
                    try {
                        if (skipAttempts > 0) {
                            folder.moveChildCursorTo(count + skipAttempts);
                            log.debug("Skipped {} corrupt node(s) at position {}/{} in '{}'",
                                    skipAttempts, count, total, folder.getDisplayName());
                        }
                        item = (PSTMessage) folder.getNextChild();
                        break;
                    } catch (Exception e) {
                        if (skipAttempts == 0) {
                            log.debug("Corrupt node at {}/{} in '{}', skipping: {}",
                                    count, total, folder.getDisplayName(), e.getMessage());
                        }
                        skipAttempts++;
                    }
                }
                if (skipAttempts > 100) {
                    log.warn("100+ consecutive corrupt nodes at {}/{} in '{}', stopping",
                            count, total, folder.getDisplayName());
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Could not iterate folder '{}': {}", folder.getDisplayName(), e.getMessage());
        }

        log.info("Scanned {}/{} items in '{}', {} matched range ({} skipped by date)",
                count, total, folder.getDisplayName(), result.size(), skipped);
    }

    private boolean quickInRange(Date start, Date end, DateRange range) {
        if (start == null) return false;
        Instant s = start.toInstant();
        Instant e = end != null ? end.toInstant() : s;
        return !s.isAfter(range.to().toInstant()) && !e.isBefore(range.from().toInstant());
    }

    // ── Recurring event expansion ─────────────────────────────────────────────────

    /**
     * MS-OXOCAL RecurrencePattern blob layout:
     *   0  ReaderVersion (2), WriterVersion (2)
     *   4  RecurFrequency (2), PatternType (2)
     *   8  CalendarType (2)
     *  10  FirstDateTime (4), Period (4), SlidingFlag (4)
     *  22  PatternTypeSpecific (variable)
     *      EndType (4), OccurrenceCount (4), FirstDOW (4)
     *      DeletedInstanceCount (4), DeletedInstanceDates[] (4 each)
     *      ModifiedInstanceCount (4), ModifiedInstanceDates[] (4 each)
     *      StartDate (4), EndDate (4)  ← always last 8 bytes
     */
    private void expandRecurring(PSTAppointment appt, DateRange range, List<OutlookEvent> result) {
        try {
            byte[] blob = appt.getRecurrenceStructure();
            if (blob == null || blob.length < 30) {
                fallbackRecurring(appt, range, result);
                return;
            }

            ByteBuffer buf = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
            short recurFreq  = buf.getShort(4);
            short patternType = buf.getShort(6);
            int   period     = buf.getInt(14);  // correct offset: 10(FirstDateTime) + 4 = 14

            // PatternTypeSpecific length depends on patternType
            int patSpecLen = switch (patternType) {
                case 0x0000 -> 0;   // Day
                case 0x0001 -> 4;   // Week (bitmask)
                case 0x0002 -> 4;   // Month (day of month)
                case 0x0003 -> 8;   // MonthNth
                case 0x0004 -> 4;   // MonthEnd
                case 0x000B -> 4;   // HjMonth
                case 0x000C -> 8;   // HjMonthNth
                case 0x000D -> 4;   // HjMonthEnd
                default     -> 4;
            };

            // For weekly recurrences, read the day-of-week bitmask (bits 0-6 = Sun-Sat)
            // MS-OXOCAL: Bit0=Sun, Bit1=Mon, Bit2=Tue, Bit3=Wed, Bit4=Thu, Bit5=Fri, Bit6=Sat
            int weekDayMask = (patternType == 0x0001 && blob.length >= 26)
                    ? (buf.getInt(22) & 0x7F) : 0;

            int pos = 22 + patSpecLen;
            if (blob.length < pos + 20) {
                fallbackRecurring(appt, range, result);
                return;
            }

            // pos: EndType(4), OccurrenceCount(4), FirstDOW(4), DeletedInstanceCount(4)
            int deletedCount = buf.getInt(pos + 12);
            int deletedBase  = pos + 16;

            // Build set of deleted occurrence dates (minutes from 1601-01-01 midnight UTC)
            Set<Long> deletedMinutes = new HashSet<>();
            for (int i = 0; i < deletedCount && deletedBase + (long)i * 4 + 4 <= blob.length; i++) {
                deletedMinutes.add(buf.getInt(deletedBase + i * 4) & 0xFFFFFFFFL);
            }

            // Walk past DeletedInstanceDates → ModifiedInstanceCount → ModifiedInstanceDates
            int afterDeleted = deletedBase + deletedCount * 4;
            if (afterDeleted + 4 > blob.length) {
                fallbackRecurring(appt, range, result);
                return;
            }
            int modifiedCount = buf.getInt(afterDeleted);
            // StartDate and EndDate immediately follow ModifiedInstanceDates
            int startDateOffset = afterDeleted + 4 + modifiedCount * 4;
            int endDateOffset   = startDateOffset + 4;
            if (endDateOffset + 4 > blob.length) {
                fallbackRecurring(appt, range, result);
                return;
            }

            int endDateMin = buf.getInt(endDateOffset);
            long endEpochSec = ((endDateMin & 0xFFFFFFFFL) - MINUTES_1601_TO_EPOCH) * 60;
            ZonedDateTime recurrenceEnd = (endEpochSec > 0 && endEpochSec < 32503680000L) // < year 3000
                    ? Instant.ofEpochSecond(endEpochSec).atZone(ZoneOffset.UTC)
                    : range.to();

            Date baseDate = appt.getStartTime();
            if (baseDate == null) return;

            ZoneId zone         = resolveZone(appt.getStartTimeZone());
            ZonedDateTime baseStart = baseDate.toInstant().atZone(zone);
            ZonedDateTime baseEnd   = appt.getEndTime() != null
                    ? appt.getEndTime().toInstant().atZone(zone)
                    : baseStart.plusHours(1);
            Duration duration = Duration.between(baseStart, baseEnd);

            // Quick overlap check
            if (baseStart.isAfter(range.to()) || recurrenceEnd.isBefore(range.from())) {
                log.debug("Recurring '{}' outside range (base={}, recEnd={})", appt.getSubject(), baseStart, recurrenceEnd);
                return;
            }

            String subject  = appt.getSubject();
            String body     = appt.getBody();
            String location = appt.getLocation();
            boolean allDay  = appt.getSubType();
            String baseId   = String.valueOf(appt.getDescriptorNodeId());
            int colorIndex  = appt.getColor();

            // Log full recurrence meta so we can diagnose unexpected inclusions
            log.debug("Recurring '{}': freq=0x{} period={} patternType=0x{} weekDayMask=0x{} base={} recEnd={} deletedInstances={} pattern={}",
                    subject,
                    Integer.toHexString(recurFreq & 0xFFFF),
                    period,
                    Integer.toHexString(patternType & 0xFFFF),
                    Integer.toHexString(weekDayMask),
                    baseStart,
                    recurrenceEnd,
                    deletedCount,
                    appt.getRecurrencePattern());

            ZonedDateTime cutoff = recurrenceEnd.isBefore(range.to()) ? recurrenceEnd : range.to();
            int safetyLimit = 2000, generated = 0;

            if (recurFreq == 0x200B && weekDayMask != 0) {
                // ── Weekly with day-of-week bitmask ─────────────────────────────────────
                // Walk week-by-week (each period weeks), emitting all active days within
                // each week in order.
                // First, find the Monday of the week containing baseStart
                ZonedDateTime weekStart = baseStart.with(java.time.DayOfWeek.MONDAY)
                        .withHour(baseStart.getHour())
                        .withMinute(baseStart.getMinute())
                        .withSecond(baseStart.getSecond());
                // MS-OXOCAL day bits: 0=Sun,1=Mon,2=Tue,3=Wed,4=Thu,5=Fri,6=Sat
                // Java DayOfWeek: MONDAY=1..SUNDAY=7
                int[] msBitByJavaDow = {0, 1, 2, 3, 4, 5, 6, 0}; // index by Java dow 1-7
                int[] javaDowByMsBit = {7, 1, 2, 3, 4, 5, 6};     // bit 0=Sun→7, bit1=Mon→1…
                while (!weekStart.isAfter(cutoff) && generated < safetyLimit) {
                    // Emit each active day of this week
                    for (int bit = 0; bit < 7; bit++) {
                        if ((weekDayMask & (1 << bit)) == 0) continue;
                        int javaDow = javaDowByMsBit[bit]; // 1=Mon..7=Sun
                        ZonedDateTime occStart = weekStart.with(java.time.DayOfWeek.of(javaDow));
                        if (occStart.isBefore(baseStart)) continue; // before series start
                        if (occStart.isAfter(cutoff)) break;
                        ZonedDateTime midnight = occStart.withZoneSameInstant(ZoneOffset.UTC)
                                .toLocalDate().atStartOfDay(ZoneOffset.UTC);
                        long occMinutes = midnight.toEpochSecond() / 60 + MINUTES_1601_TO_EPOCH;
                        if (deletedMinutes.contains(occMinutes)) {
                            log.debug("  Skipping deleted occurrence of '{}' at {} (occMinutes={})", subject, occStart, occMinutes);
                            continue;
                        }
                        ZonedDateTime occEnd = occStart.plus(duration);
                        if (!occStart.isAfter(range.to()) && !occEnd.isBefore(range.from())) {
                            log.debug("  Including occurrence of '{}' at {} (occMinutes={})", subject, occStart, occMinutes);
                            result.add(new OutlookEvent(
                                    baseId + "_" + occStart.toEpochSecond(),
                                    subject, body, location, occStart, occEnd, allDay, colorIndex));
                            generated++;
                        }
                    }
                    weekStart = weekStart.plusWeeks(Math.max(1, period));
                }
            } else {
                // ── Daily / Monthly / Yearly (simple step) ──────────────────────────────
                ZonedDateTime occStart = baseStart;
                while (!occStart.isAfter(cutoff) && generated < safetyLimit) {
                    ZonedDateTime midnight = occStart.withZoneSameInstant(ZoneOffset.UTC)
                            .toLocalDate().atStartOfDay(ZoneOffset.UTC);
                    long occMinutes = midnight.toEpochSecond() / 60 + MINUTES_1601_TO_EPOCH;
                    if (deletedMinutes.contains(occMinutes)) {
                        log.debug("  Skipping deleted occurrence of '{}' at {} (occMinutes={})", subject, occStart, occMinutes);
                    } else {
                        ZonedDateTime occEnd = occStart.plus(duration);
                        if (!occStart.isAfter(range.to()) && !occEnd.isBefore(range.from())) {
                            log.debug("  Including occurrence of '{}' at {} (occMinutes={})", subject, occStart, occMinutes);
                            result.add(new OutlookEvent(
                                    baseId + "_" + occStart.toEpochSecond(),
                                    subject, body, location, occStart, occEnd, allDay, colorIndex));
                            generated++;
                        }
                    }
                    occStart = nextOccurrence(occStart, recurFreq, period);
                    if (occStart == null) break;
                }
            }
            if (generated > 0) {
                log.debug("Expanded recurring '{}' → {} occurrences in range", subject, generated);
            } else {
                log.debug("Recurring '{}' had 0 occurrences in range after deleted-instance filtering", subject);
            }
        } catch (Exception e) {
            log.debug("Failed to expand recurrence for '{}': {}", appt.getSubject(), e.getMessage());
            fallbackRecurring(appt, range, result);
        }
    }

    /** Last resort: include only if the base event date overlaps the range. */
    private void fallbackRecurring(PSTAppointment appt, DateRange range, List<OutlookEvent> result) {
        try {
            if (!quickInRange(appt.getStartTime(), appt.getEndTime(), range)) return;
            OutlookEvent event = toOutlookEvent(appt);
            if (event != null) result.add(event);
        } catch (Exception e) {
            log.debug("Fallback recurring parse failed: {}", e.getMessage());
        }
    }

    private ZonedDateTime nextOccurrence(ZonedDateTime current, short recurFreq, int period) {
        int p = Math.max(1, period);
        return switch (recurFreq) {
            case 0x200A -> current.plusDays(p);          // Daily
            case 0x200B -> current.plusWeeks(p);         // Weekly
            case 0x200C, 0x200D -> current.plusMonths(p);// Monthly
            case 0x200E -> current.plusMonths(p);        // Yearly (period=12 months)
            default     -> current.plusDays(1);
        };
    }

    // ── Event conversion ──────────────────────────────────────────────────────────

    private OutlookEvent toOutlookEvent(PSTAppointment appt) {
        try {
            String id       = String.valueOf(appt.getDescriptorNodeId());
            String subject  = appt.getSubject();
            String body     = appt.getBody();
            String location = appt.getLocation();
            boolean allDay  = appt.getSubType();
            ZoneId zone     = resolveZone(appt.getStartTimeZone());
            ZonedDateTime start = toZdt(appt.getStartTime(), zone);
            ZonedDateTime end   = toZdt(appt.getEndTime(),   zone);
            if (start == null) return null;
            return new OutlookEvent(id, subject, body, location, start, end, allDay, appt.getColor());
        } catch (Exception e) {
            log.debug("Skipping appointment due to parse error: {}", e.getMessage());
            return null;
        }
    }

    private ZoneId resolveZone(com.pff.PSTTimeZone pstTz) {
        if (pstTz == null) return ZoneId.of("UTC");
        String name = pstTz.getName();
        if (name != null && !name.isBlank()) {
            try { return ZoneId.of(name); } catch (Exception ignored) {}
            ZoneId mapped = WINDOWS_TZ_MAP.get(name);
            if (mapped != null) return mapped;
        }
        java.util.SimpleTimeZone stz = pstTz.getSimpleTimeZone();
        if (stz != null) { try { return stz.toZoneId(); } catch (Exception ignored) {} }
        return ZoneId.of("UTC");
    }

    private static final Map<String, ZoneId> WINDOWS_TZ_MAP = Map.ofEntries(
        Map.entry("Central European Standard Time", ZoneId.of("Europe/Warsaw")),
        Map.entry("Central European Time",          ZoneId.of("Europe/Warsaw")),
        Map.entry("Eastern Standard Time",          ZoneId.of("America/New_York")),
        Map.entry("Eastern Time",                   ZoneId.of("America/New_York")),
        Map.entry("Pacific Standard Time",          ZoneId.of("America/Los_Angeles")),
        Map.entry("Pacific Time",                   ZoneId.of("America/Los_Angeles")),
        Map.entry("GMT Standard Time",              ZoneId.of("Europe/London")),
        Map.entry("Greenwich Mean Time",            ZoneId.of("Europe/London")),
        Map.entry("UTC",                            ZoneId.of("UTC")),
        Map.entry("W. Europe Standard Time",        ZoneId.of("Europe/Berlin")),
        Map.entry("Romance Standard Time",          ZoneId.of("Europe/Paris")),
        Map.entry("India Standard Time",            ZoneId.of("Asia/Kolkata")),
        Map.entry("IST",                            ZoneId.of("Asia/Kolkata"))
    );

    private ZonedDateTime toZdt(Date date, ZoneId zone) {
        if (date == null) return null;
        return date.toInstant().atZone(zone);
    }

    private void validateProfilePath(String profilePath) {
        if (profilePath == null || profilePath.isBlank())
            throw new IllegalArgumentException("Outlook profile path is not configured");
        File file = new File(profilePath);
        if (!file.exists() || !file.canRead())
            throw new IllegalArgumentException(
                    "Outlook profile path does not exist or is not readable: " + profilePath);
    }
}
