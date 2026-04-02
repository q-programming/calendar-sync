package pl.qprogramming.calendarsync.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pl.qprogramming.calendarsync.port.DateRange;
import pl.qprogramming.calendarsync.port.OutlookEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OutlookCalendarAdapter recurrence exception handling.
 *
 * Covers:
 *  - localMinToZdt(): local-timezone minutes-from-1601 → ZonedDateTime
 *  - readExceptionOccurrences(): MS-OXOCAL ExceptionInfo blob parsing
 *    - Moved instance (no overrides)
 *    - Moved instance with overridden subject + location
 *    - Exception outside date range → excluded
 *    - Empty ExceptionCount (blob too short)
 *    - Multiple exceptions, some in range, some not
 */
class OutlookCalendarAdapterExceptionTest {

    // MS-OXOCAL: minutes from 1601-01-01T00:00Z to Unix epoch
    static final long MIN_1601 = 194074560L;

    // Convenience: convert LocalDateTime (wall clock) to ExceptionInfo minutes
    static long toLocalMin(LocalDateTime ldt) {
        return ldt.toEpochSecond(ZoneOffset.UTC) / 60 + MIN_1601;
    }

    // Convenience: convert LocalDateTime at midnight to "original start date" minutes (UTC midnight)
    static long toOrigMin(LocalDate date) {
        return date.atStartOfDay().toEpochSecond(ZoneOffset.UTC) / 60 + MIN_1601;
    }

    static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    static final ZoneId WARSAW   = ZoneId.of("Europe/Warsaw");

    OutlookCalendarAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OutlookCalendarAdapter();
    }

    // ── localMinToZdt ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("localMinToZdt — local-time minutes from 1601 to ZonedDateTime")
    class LocalMinToZdt {

        @Test
        @DisplayName("09:00 EDT on Apr 3 2026 → correct instant (UTC 13:00)")
        void nineAmEdtApril3() {
            long min = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 0));
            ZonedDateTime zdt = adapter.localMinToZdt(min, NEW_YORK);

            assertThat(zdt.getHour()).isEqualTo(9);
            assertThat(zdt.getMinute()).isEqualTo(0);
            assertThat(zdt.toInstant()).isEqualTo(Instant.parse("2026-04-03T13:00:00Z"));
        }

        @Test
        @DisplayName("09:00 Warsaw (CEST=+2) on Apr 3 2026 → UTC 07:00")
        void nineAmWarsawApril3() {
            long min = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 0));
            ZonedDateTime zdt = adapter.localMinToZdt(min, WARSAW);

            assertThat(zdt.getHour()).isEqualTo(9);
            assertThat(zdt.toInstant()).isEqualTo(Instant.parse("2026-04-03T07:00:00Z"));
        }

        @Test
        @DisplayName("same wall-clock, different timezone → different UTC instants")
        void sameWallClockDifferentZone() {
            long min = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 0));
            ZonedDateTime inNY     = adapter.localMinToZdt(min, NEW_YORK); // UTC-4 → 13:00Z
            ZonedDateTime inWarsaw = adapter.localMinToZdt(min, WARSAW);   // UTC+2 → 07:00Z

            assertThat(inNY.toInstant()).isNotEqualTo(inWarsaw.toInstant());
        }
    }

    // ── readExceptionOccurrences ──────────────────────────────────────────────

    @Nested
    @DisplayName("readExceptionOccurrences — ExceptionInfo blob parsing")
    class ReadExceptionOccurrences {

        /** Build minimal AppointmentRecurrencePattern blob starting at offset 0.
         *  Layout: WriterVersion2(4) ReaderVersion2(4) StartOffset(4) EndOffset(4) ExceptionCount(2)
         *          ExceptionInfo[...] */
        ByteBuffer newArBlob(int exCount) {
            int size = 18 + exCount * 14; // header + fixed-size exceptions (no overrides)
            ByteBuffer b = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN);
            b.putInt(0x00003009); // WriterVersion2
            b.putInt(0x00003009); // ReaderVersion2
            b.putInt(540);        // StartTimeOffset (09:00 = 540 min)
            b.putInt(570);        // EndTimeOffset   (09:30)
            b.putShort((short) exCount);
            return b;
        }

        void writeExceptionInfo(ByteBuffer b, long startMin, long endMin, long origMin, int overFlags) {
            b.putInt((int) startMin);
            b.putInt((int) endMin);
            b.putInt((int) origMin);
            b.putShort((short) overFlags);
        }

        DateRange rangeAround(LocalDate from, LocalDate to) {
            return new DateRange(
                from.atStartOfDay(ZoneOffset.UTC),
                to.atStartOfDay(ZoneOffset.UTC));
        }

        @Test
        @DisplayName("one moved exception (no overrides) in range → included with new time")
        void singleExceptionInRange() {
            // Original: Apr 2 at 09:00 EDT; moved to Apr 3 at 09:00 EDT
            long startMin = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 0));
            long endMin   = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 30));
            long origMin  = toOrigMin(LocalDate.of(2026, 4, 2));

            ByteBuffer b = newArBlob(1);
            writeExceptionInfo(b, startMin, endMin, origMin, 0);

            List<OutlookEvent> result = new ArrayList<>();
            adapter.readExceptionOccurrences(b.array(), 0, NEW_YORK,
                    "base123", "Standup", "body", "Zoom", false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(result).hasSize(1);
            OutlookEvent ev = result.get(0);
            assertThat(ev.id()).isEqualTo("base123_ex_" + origMin);
            assertThat(ev.subject()).isEqualTo("Standup");
            assertThat(ev.location()).isEqualTo("Zoom");
            assertThat(ev.start().toInstant()).isEqualTo(Instant.parse("2026-04-03T13:00:00Z")); // 09:00 EDT = 13:00Z
            assertThat(ev.start().getHour()).isEqualTo(9);
        }

        @Test
        @DisplayName("exception outside range → not included")
        void exceptionOutsideRange() {
            long startMin = toLocalMin(LocalDateTime.of(2026, 5, 1, 9, 0));
            long endMin   = toLocalMin(LocalDateTime.of(2026, 5, 1, 9, 30));
            long origMin  = toOrigMin(LocalDate.of(2026, 4, 2));

            ByteBuffer b = newArBlob(1);
            writeExceptionInfo(b, startMin, endMin, origMin, 0);

            List<OutlookEvent> result = new ArrayList<>();
            adapter.readExceptionOccurrences(b.array(), 0, NEW_YORK,
                    "base123", "Standup", "body", null, false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("exception with overridden subject (ARO_SUBJECT) → uses exception subject")
        void overriddenSubject() {
            long startMin = toLocalMin(LocalDateTime.of(2026, 4, 3, 15, 0));
            long endMin   = toLocalMin(LocalDateTime.of(2026, 4, 3, 16, 0));
            long origMin  = toOrigMin(LocalDate.of(2026, 4, 2));

            String newSubject = "Special Session";
            byte[] subjBytes  = newSubject.getBytes(StandardCharsets.ISO_8859_1);
            int totalSize = 18 + 14 + 4 + subjBytes.length; // header + exception base + SubjLen(2)+SubjLen2(2) + bytes
            ByteBuffer b = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);
            b.putInt(0x00003009); b.putInt(0x00003009); b.putInt(540); b.putInt(570);
            b.putShort((short) 1); // ExceptionCount
            b.putInt((int) startMin); b.putInt((int) endMin); b.putInt((int) origMin);
            b.putShort((short) 0x0001); // ARO_SUBJECT
            b.putShort((short) (subjBytes.length + 1)); // SubjectLength (includes null terminator)
            b.putShort((short) (subjBytes.length + 1)); // SubjectLength2
            b.put(subjBytes);

            List<OutlookEvent> result = new ArrayList<>();
            adapter.readExceptionOccurrences(b.array(), 0, NEW_YORK,
                    "base123", "Original Subject", "body", null, false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).subject()).isEqualTo("Special Session");
        }

        @Test
        @DisplayName("exception with overridden location (ARO_LOCATION) → uses exception location")
        void overriddenLocation() {
            long startMin = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 0));
            long endMin   = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 30));
            long origMin  = toOrigMin(LocalDate.of(2026, 4, 2));

            String newLoc  = "Conference Room B";
            byte[] locBytes = newLoc.getBytes(StandardCharsets.ISO_8859_1);
            int totalSize = 18 + 14 + 4 + locBytes.length;
            ByteBuffer b = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);
            b.putInt(0x00003009); b.putInt(0x00003009); b.putInt(540); b.putInt(570);
            b.putShort((short) 1);
            b.putInt((int) startMin); b.putInt((int) endMin); b.putInt((int) origMin);
            b.putShort((short) 0x0010); // ARO_LOCATION
            b.putShort((short) (locBytes.length + 1)); // LocationLength
            b.putShort((short) (locBytes.length + 1)); // LocationLength2
            b.put(locBytes);

            List<OutlookEvent> result = new ArrayList<>();
            adapter.readExceptionOccurrences(b.array(), 0, NEW_YORK,
                    "base123", "Meeting", "body", "Room A", false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).location()).isEqualTo("Conference Room B");
        }

        @Test
        @DisplayName("zero exceptions → empty result")
        void zeroExceptions() {
            ByteBuffer b = newArBlob(0);

            List<OutlookEvent> result = new ArrayList<>();
            adapter.readExceptionOccurrences(b.array(), 0, NEW_YORK,
                    "base", "Sub", "body", null, false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("blob too short (< 18 bytes) → returns 0, no crash")
        void blobTooShort() {
            byte[] tiny = new byte[10];
            List<OutlookEvent> result = new ArrayList<>();
            int count = adapter.readExceptionOccurrences(tiny, 0, NEW_YORK,
                    "base", "Sub", "body", null, false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(count).isZero();
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("two exceptions — one in range, one not → only one added")
        void twoExceptionsPartiallyInRange() {
            long start1 = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 0));
            long end1   = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 30));
            long orig1  = toOrigMin(LocalDate.of(2026, 4, 2));

            long start2 = toLocalMin(LocalDateTime.of(2026, 5, 1, 9, 0)); // outside range
            long end2   = toLocalMin(LocalDateTime.of(2026, 5, 1, 9, 30));
            long orig2  = toOrigMin(LocalDate.of(2026, 4, 30));

            ByteBuffer b = newArBlob(2);
            writeExceptionInfo(b, start1, end1, orig1, 0);
            writeExceptionInfo(b, start2, end2, orig2, 0);

            List<OutlookEvent> result = new ArrayList<>();
            int count = adapter.readExceptionOccurrences(b.array(), 0, NEW_YORK,
                    "base", "Sub", "body", null, false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(count).isEqualTo(1);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).contains("" + orig1);
        }

        @Test
        @DisplayName("exception ID format: baseId + '_ex_' + origMin")
        void exceptionIdFormat() {
            long startMin = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 0));
            long endMin   = toLocalMin(LocalDateTime.of(2026, 4, 3, 9, 30));
            long origMin  = toOrigMin(LocalDate.of(2026, 4, 2));

            ByteBuffer b = newArBlob(1);
            writeExceptionInfo(b, startMin, endMin, origMin, 0);

            List<OutlookEvent> result = new ArrayList<>();
            adapter.readExceptionOccurrences(b.array(), 0, NEW_YORK,
                    "node999", "Sub", "body", null, false, 0,
                    rangeAround(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 16)), result);

            assertThat(result.get(0).id()).isEqualTo("node999_ex_" + origMin);
        }
    }
}
