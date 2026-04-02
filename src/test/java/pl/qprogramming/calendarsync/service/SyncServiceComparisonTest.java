package pl.qprogramming.calendarsync.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.qprogramming.calendarsync.port.*;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SyncService event comparison logic.
 *
 * Covers:
 *  - eqInstant(): same UTC instant but different zone IDs must be equal
 *  - isChanged(): detects real differences, ignores zone-label differences
 */
@ExtendWith(MockitoExtension.class)
class SyncServiceComparisonTest {

    @Mock OutlookCalendarPort outlookPort;
    @Mock GoogleCalendarPort googlePort;
    @Mock ProfileService profileService;
    @Mock SettingsService settingsService;
    @Mock LogService logService;

    SyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = new SyncService(outlookPort, googlePort, profileService, settingsService, logService);
    }

    // ── eqInstant ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("eqInstant — zone-aware instant comparison")
    class EqInstant {

        @Test
        @DisplayName("same instant, different zone IDs (Europe/Warsaw vs +02:00) → equal")
        void sameInstantDifferentZoneLabels() {
            ZonedDateTime warsaw = ZonedDateTime.of(2026, 4, 3, 9, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
            ZonedDateTime offset = ZonedDateTime.of(2026, 4, 3, 9, 0, 0, 0, ZoneOffset.of("+02:00"));

            assertThat(syncService.eqInstant(warsaw, offset)).isTrue();
        }

        @Test
        @DisplayName("same zone, different time → not equal")
        void differentTimeNotEqual() {
            ZonedDateTime t1 = ZonedDateTime.of(2026, 4, 3, 9, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
            ZonedDateTime t2 = ZonedDateTime.of(2026, 4, 3, 10, 0, 0, 0, ZoneId.of("Europe/Warsaw"));

            assertThat(syncService.eqInstant(t1, t2)).isFalse();
        }

        @Test
        @DisplayName("same wall-clock, different zones with different UTC offsets → not equal")
        void sameWallClockDifferentZones() {
            // 09:00 Warsaw (UTC+2) vs 09:00 London (UTC+1) — different instants
            ZonedDateTime warsaw = ZonedDateTime.of(2026, 4, 3, 9, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
            ZonedDateTime london = ZonedDateTime.of(2026, 4, 3, 9, 0, 0, 0, ZoneId.of("Europe/London"));

            assertThat(syncService.eqInstant(warsaw, london)).isFalse();
        }

        @Test
        @DisplayName("both null → equal")
        void bothNullEqual() {
            assertThat(syncService.eqInstant(null, null)).isTrue();
        }

        @Test
        @DisplayName("one null → not equal")
        void oneNullNotEqual() {
            ZonedDateTime t = ZonedDateTime.now();
            assertThat(syncService.eqInstant(t, null)).isFalse();
            assertThat(syncService.eqInstant(null, t)).isFalse();
        }
    }

    // ── isChanged ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isChanged — event diff detection")
    class IsChanged {

        static final ZonedDateTime START_WARSAW = ZonedDateTime.of(2026, 4, 3, 9, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
        static final ZonedDateTime END_WARSAW   = START_WARSAW.plusHours(1);
        // Same instants, but with offset zone ID (as Google API returns)
        static final ZonedDateTime START_OFFSET = START_WARSAW.withZoneSameInstant(ZoneOffset.of("+02:00"));
        static final ZonedDateTime END_OFFSET   = END_WARSAW.withZoneSameInstant(ZoneOffset.of("+02:00"));

        OutlookEvent outlookEvent(String subject, String location, ZonedDateTime start, ZonedDateTime end) {
            return new OutlookEvent("id1", subject, "body", location, start, end, false, 0);
        }

        GoogleEvent googleEvent(String summary, String location, ZonedDateTime start, ZonedDateTime end) {
            return new GoogleEvent("gid1", "id1", summary, "body", location, start, end, false);
        }

        @Test
        @DisplayName("identical events (zone label differs: Warsaw vs +02:00) → not changed")
        void identicalEventsDifferentZoneLabels() {
            OutlookEvent oe = outlookEvent("Basen", null, START_WARSAW, END_WARSAW);
            GoogleEvent ge  = googleEvent("Basen", null, START_OFFSET, END_OFFSET);

            assertThat(syncService.isChanged(oe, ge)).isFalse();
        }

        @Test
        @DisplayName("subject changed → changed")
        void subjectChanged() {
            OutlookEvent oe = outlookEvent("Basen Updated", null, START_WARSAW, END_WARSAW);
            GoogleEvent ge  = googleEvent("Basen", null, START_OFFSET, END_OFFSET);

            assertThat(syncService.isChanged(oe, ge)).isTrue();
        }

        @Test
        @DisplayName("location added → changed")
        void locationAdded() {
            OutlookEvent oe = outlookEvent("Basen", "Pool Room 1", START_WARSAW, END_WARSAW);
            GoogleEvent ge  = googleEvent("Basen", null, START_OFFSET, END_OFFSET);

            assertThat(syncService.isChanged(oe, ge)).isTrue();
        }

        @Test
        @DisplayName("start time shifted by 1h (DST regression check) → changed")
        void startTimeShiftedOneHour() {
            ZonedDateTime wrongStart = START_WARSAW.plusHours(1); // the DST bug: 10:00 instead of 09:00
            OutlookEvent oe = outlookEvent("Basen", null, START_WARSAW, END_WARSAW);
            GoogleEvent ge  = googleEvent("Basen", null, wrongStart, wrongStart.plusHours(1));

            assertThat(syncService.isChanged(oe, ge)).isTrue();
        }

        @Test
        @DisplayName("null location on both sides → not changed")
        void bothLocationsNull() {
            OutlookEvent oe = outlookEvent("Meeting", null, START_WARSAW, END_WARSAW);
            GoogleEvent ge  = googleEvent("Meeting", null, START_OFFSET, END_OFFSET);

            assertThat(syncService.isChanged(oe, ge)).isFalse();
        }

        @Test
        @DisplayName("empty string location vs null → not changed (normalized)")
        void emptyVsNullLocation() {
            // Outlook returns "" for no location; Google returns null — must be treated equal
            OutlookEvent oe = outlookEvent("Meeting", "", START_WARSAW, END_WARSAW);
            GoogleEvent ge  = googleEvent("Meeting", null, START_OFFSET, END_OFFSET);

            assertThat(syncService.isChanged(oe, ge)).isFalse();
        }
    }
}
