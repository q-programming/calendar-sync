package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.SyncLogEntryEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.port.*;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final OutlookCalendarPort outlookPort;
    private final GoogleCalendarPort googlePort;
    private final ProfileService profileService;
    private final SettingsService settingsService;
    private final LogService logService;

    public SyncRunEntity runSync(Principal principal) {
        SyncSettingsEntity settings = settingsService.getOrCreate();
        var profile = profileService.getOrCreate();

        SyncRunEntity run = new SyncRunEntity();
        run.setId(UUID.randomUUID().toString());
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setStatus("RUNNING");
        run.setCreated(0);
        run.setUpdated(0);
        run.setDeleted(0);
        logService.saveRun(run);

        RunLogger logger = new RunLogger(run.getId(), settings.isDebugLogging(), logService);

        try {
            String outlookPath = profile.getOutlookProfilePath();
            String outlookCalendarId = profile.getOutlookCalendarId();
            String googleCalendarId = profile.getGoogleCalendarId();

            if (outlookPath == null || outlookPath.isBlank()) {
                throw new IllegalStateException("Outlook profile path is not configured");
            }
            if (outlookCalendarId == null || outlookCalendarId.isBlank()) {
                throw new IllegalStateException("Outlook calendar is not selected");
            }
            if (googleCalendarId == null || googleCalendarId.isBlank()) {
                throw new IllegalStateException("Google calendar is not selected");
            }

            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            DateRange range = new DateRange(
                    now.minusDays(settings.getDaysPast()),
                    now.plusDays(settings.getDaysFuture()));

            logger.info("Starting sync. Range: %s to %s", range.from(), range.to());

            List<OutlookEvent> outlookEvents = outlookPort.readEvents(outlookPath, outlookCalendarId, range);
            logger.info("Read %d Outlook events", outlookEvents.size());

            List<GoogleEvent> googleEvents = googlePort.readEvents(principal, googleCalendarId, range);
            logger.info("Read %d Google (sync-managed) events", googleEvents.size());

            Map<String, GoogleEvent> googleByOutlookId = googleEvents.stream()
                    .filter(e -> e.outlookId() != null)
                    .collect(Collectors.toMap(GoogleEvent::outlookId, e -> e, (a, b) -> a));

            Map<String, OutlookEvent> outlookById = outlookEvents.stream()
                    .collect(Collectors.toMap(OutlookEvent::id, e -> e, (a, b) -> a));

            int created = 0, updated = 0, deleted = 0;

            for (OutlookEvent oe : outlookEvents) {
                GoogleEvent existing = googleByOutlookId.get(oe.id());
                if (existing == null) {
                    logger.debug("Creating event: %s", oe.subject());
                    googlePort.upsertEvent(principal, googleCalendarId, oe, null);
                    created++;
                } else if (isChanged(oe, existing)) {
                    logger.debug("Updating event: %s", oe.subject());
                    googlePort.upsertEvent(principal, googleCalendarId, oe, existing.id());
                    updated++;
                }
            }

            for (GoogleEvent ge : googleEvents) {
                if (ge.outlookId() != null && !outlookById.containsKey(ge.outlookId())) {
                    logger.debug("Deleting removed event: %s", ge.summary());
                    googlePort.deleteEvent(principal, googleCalendarId, ge.id());
                    deleted++;
                }
            }

            run.setCreated(created);
            run.setUpdated(updated);
            run.setDeleted(deleted);
            run.setStatus("SUCCESS");
            run.setMessage(String.format("Sync completed: +%d ~%d -%d", created, updated, deleted));
            logger.info("Sync complete: created=%d, updated=%d, deleted=%d", created, updated, deleted);
        } catch (Exception e) {
            log.error("Sync failed", e);
            run.setStatus("FAILED");
            run.setMessage(e.getMessage());
            logger.error("Sync failed: %s", e.getMessage());
        } finally {
            run.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
            logService.saveRun(run);
        }
        return run;
    }

    private boolean isChanged(OutlookEvent oe, GoogleEvent ge) {
        return !eq(oe.subject(), ge.summary())
                || !eq(oe.location(), ge.location())
                || !eq(oe.start(), ge.start())
                || !eq(oe.end(), ge.end());
    }

    private boolean eq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static class RunLogger {
        private final String runId;
        private final boolean debug;
        private final LogService logService;

        RunLogger(String runId, boolean debug, LogService logService) {
            this.runId = runId;
            this.debug = debug;
            this.logService = logService;
        }

        void info(String fmt, Object... args) { persist("INFO", fmt, args); }
        void debug(String fmt, Object... args) { if (debug) persist("DEBUG", fmt, args); }
        void warn(String fmt, Object... args) { persist("WARN", fmt, args); }
        void error(String fmt, Object... args) { persist("ERROR", fmt, args); }

        private void persist(String level, String fmt, Object[] args) {
            String msg = args.length == 0 ? fmt : String.format(fmt, args);
            SyncLogEntryEntity entry = new SyncLogEntryEntity();
            entry.setRunId(runId);
            entry.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
            entry.setLevel(level);
            entry.setMessage(msg);
            logService.saveEntry(entry);
        }
    }
}
