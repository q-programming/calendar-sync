package pl.qprogramming.calendarsync.adapter;

import com.pff.PSTTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.time.Instant;
import java.util.SimpleTimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for timezone resolution logic in OutlookCalendarAdapter.
 *
 * Key design decisions under test:
 *  - resolveZone()     uses fixed-offset SimpleTimeZone (libpst already applied standard offset)
 *  - resolveIanaZone() uses DST-aware IANA zone (recurrence expansion must preserve wall-clock)
 */
@ExtendWith(MockitoExtension.class)
class OutlookCalendarAdapterTimezoneTest {

    @Mock
    PSTTimeZone pstTz;

    OutlookCalendarAdapter adapter;

    // CET standard offset: UTC+1 — using numeric ID so toZoneId() returns a fixed offset
    static final SimpleTimeZone CET_SIMPLE = new SimpleTimeZone(3600_000, "GMT+01:00");

    @BeforeEach
    void setUp() {
        adapter = new OutlookCalendarAdapter();
    }

    // ── resolveZone (fixed-offset for single events) ─────────────────────────

    @Nested
    @DisplayName("resolveZone — prefers fixed-offset SimpleTimeZone")
    class ResolveZone {

        @Test
        @DisplayName("returns fixed +01:00 from SimpleTimeZone when name is Windows TZ name")
        void prefersSimpleTimeZoneOverIanaName() {
            when(pstTz.getSimpleTimeZone()).thenReturn(CET_SIMPLE);

            ZoneId zone = adapter.resolveZone(pstTz);

            // Must be fixed offset (GMT+01:00), NOT Europe/Warsaw (which is DST-aware)
            assertThat(zone.normalized()).isEqualTo(ZoneOffset.ofHours(1));
            assertThat(zone.getId()).isNotEqualTo("Europe/Warsaw");
        }

        @Test
        @DisplayName("returns UTC when PSTTimeZone is null")
        void nullReturnsUtc() {
            assertThat(adapter.resolveZone(null)).isEqualTo(ZoneId.of("UTC"));
        }

        @Test
        @DisplayName("falls back to IANA map when SimpleTimeZone is null")
        void fallsBackToIanaMapWhenNoSimpleZone() {
            when(pstTz.getSimpleTimeZone()).thenReturn(null);
            when(pstTz.getName()).thenReturn("Central European Standard Time");

            ZoneId zone = adapter.resolveZone(pstTz);

            assertThat(zone.getId()).isEqualTo("Europe/Warsaw");
        }

        @Test
        @DisplayName("falls back to UTC when name is unknown and SimpleTimeZone is null")
        void fallsBackToUtcForUnknownName() {
            when(pstTz.getSimpleTimeZone()).thenReturn(null);
            when(pstTz.getName()).thenReturn("Some Unknown TZ");

            assertThat(adapter.resolveZone(pstTz)).isEqualTo(ZoneId.of("UTC"));
        }
    }

    // ── resolveIanaZone (DST-aware for recurrence expansion) ─────────────────

    @Nested
    @DisplayName("resolveIanaZone — prefers DST-aware IANA zone")
    class ResolveIanaZone {

        @Test
        @DisplayName("returns Europe/Warsaw (DST-aware) for Central European Standard Time")
        void returnsDstAwareZoneForWindowsName() {
            when(pstTz.getName()).thenReturn("Central European Standard Time");

            ZoneId zone = adapter.resolveIanaZone(pstTz);

            assertThat(zone.getId()).isEqualTo("Europe/Warsaw");
        }

        @Test
        @DisplayName("returns UTC when PSTTimeZone is null")
        void nullReturnsUtc() {
            assertThat(adapter.resolveIanaZone(null)).isEqualTo(ZoneId.of("UTC"));
        }

        @Test
        @DisplayName("falls back to SimpleTimeZone fixed offset when name is unknown")
        void fallsBackToSimpleZoneWhenNameUnknown() {
            when(pstTz.getName()).thenReturn("Unknown TZ");
            when(pstTz.getSimpleTimeZone()).thenReturn(CET_SIMPLE);

            ZoneId zone = adapter.resolveIanaZone(pstTz);

            // SimpleTimeZone("GMT+01:00") → toZoneId() returns fixed offset zone
            assertThat(zone.normalized()).isEqualTo(ZoneOffset.ofHours(1));
        }

        @Test
        @DisplayName("recognises direct IANA name (e.g. Asia/Kolkata)")
        void recognisesDirectIanaName() {
            when(pstTz.getName()).thenReturn("Asia/Kolkata");

            ZoneId zone = adapter.resolveIanaZone(pstTz);

            assertThat(zone.getId()).isEqualTo("Asia/Kolkata");
        }
    }

    // ── DST wall-clock preservation (the core bug scenario) ──────────────────

    @Nested
    @DisplayName("DST recurrence wall-clock preservation")
    class DstWallClockPreservation {

        /**
         * Scenario: recurring meeting at 09:00 Warsaw (CET, UTC+1) base = 2026-03-06.
         * After DST (2026-03-29), the meeting should still be at 09:00 local time,
         * i.e. 07:00 UTC (CEST = UTC+2).
         *
         * resolveZone()     gives +01:00 → adding 4 weeks = 08:00 UTC (WRONG: shows as 10:00 CEST)
         * resolveIanaZone() gives Europe/Warsaw → adding 4 weeks = 07:00 UTC (CORRECT: 09:00 CEST)
         */
        @Test
        @DisplayName("resolveIanaZone preserves 09:00 wall-clock across DST boundary (+4 weeks)")
        void ianaZonePreservesWallClockAcrossDst() {
            when(pstTz.getName()).thenReturn("Central European Standard Time");

            ZoneId ianaZone = adapter.resolveIanaZone(pstTz);
            assertThat(ianaZone.getId()).isEqualTo("Europe/Warsaw");

            // Base: 2026-03-06 09:00 CET (UTC+1) = 08:00 UTC (before DST on Mar 29)
            ZonedDateTime base = ZonedDateTime.of(2026, 3, 6, 9, 0, 0, 0, ianaZone);
            assertThat(base.toInstant()).isEqualTo(Instant.parse("2026-03-06T08:00:00Z"));

            // +4 weeks = 2026-04-03, now CEST (UTC+2) → 09:00 wall-clock = 07:00 UTC
            ZonedDateTime fourWeeksLater = base.plusWeeks(4);
            assertThat(fourWeeksLater.getHour()).isEqualTo(9);
            assertThat(fourWeeksLater.toInstant()).isEqualTo(Instant.parse("2026-04-03T07:00:00Z"));
        }

        @Test
        @DisplayName("resolveZone (fixed offset) shifts wall-clock by 1h after DST (the bug case)")
        void fixedOffsetShiftsWallClockAfterDst() {
            when(pstTz.getSimpleTimeZone()).thenReturn(CET_SIMPLE);

            ZoneId fixedZone = adapter.resolveZone(pstTz); // +01:00

            // Base: 2026-03-06 09:00 +01:00 = 08:00 UTC
            ZonedDateTime base = ZonedDateTime.of(2026, 3, 6, 9, 0, 0, 0, fixedZone);
            ZonedDateTime fourWeeksLater = base.plusWeeks(4);

            // Fixed offset has no DST — still 08:00 UTC, which displays as 10:00 CEST (the bug)
            assertThat(fourWeeksLater.toInstant()).isEqualTo(Instant.parse("2026-04-03T08:00:00Z"));
            ZonedDateTime inWarsaw = fourWeeksLater.withZoneSameInstant(ZoneId.of("Europe/Warsaw"));
            assertThat(inWarsaw.getHour()).isEqualTo(10); // wrong: should be 09:00
        }

        @Test
        @DisplayName("India TZ (no DST) — resolveIanaZone wall-clock unchanged after +4 weeks")
        void noDstZoneUnaffected() {
            when(pstTz.getName()).thenReturn("India Standard Time");

            ZoneId ianaZone = adapter.resolveIanaZone(pstTz);
            assertThat(ianaZone.getId()).isEqualTo("Asia/Calcutta");

            ZonedDateTime base = ZonedDateTime.of(2026, 3, 6, 9, 30, 0, 0, ianaZone);
            ZonedDateTime fourWeeksLater = base.plusWeeks(4);

            // No DST — wall-clock stays 09:30, UTC offset constant at +05:30
            assertThat(fourWeeksLater.getHour()).isEqualTo(9);
            assertThat(fourWeeksLater.getMinute()).isEqualTo(30);
        }
    }
}
