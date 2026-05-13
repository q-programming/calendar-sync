package pl.qprogramming.calendarsync.service.outlook;

import com.pff.PSTTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pl.qprogramming.calendarsync.model.DateRange;
import pl.qprogramming.calendarsync.service.LogService;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.SimpleTimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reflection-based tests for OutlookCalendarService private helper methods
 * that contain pure Java logic with no PST file dependency.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OutlookCalendarService — private helper methods")
class OutlookCalendarServicePrivateMethodTest {

    @Mock private LogService logService;

    @InjectMocks
    private OutlookCalendarService service;

    // ── quickInRange ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("quickInRange")
    class QuickInRangeTests {

        private Method quickInRange;

        @BeforeEach
        void setUp() throws Exception {
            quickInRange = OutlookCalendarService.class
                    .getDeclaredMethod("quickInRange", Date.class, Date.class, DateRange.class);
            quickInRange.setAccessible(true);
        }

        private boolean invoke(Date start, Date end, DateRange range) throws Exception {
            return (boolean) quickInRange.invoke(service, start, end, range);
        }

        private DateRange range(String from, String to) {
            return new DateRange(
                    ZonedDateTime.parse(from + "T00:00:00Z").withZoneSameInstant(ZoneId.of("UTC")),
                    ZonedDateTime.parse(to + "T23:59:59Z").withZoneSameInstant(ZoneId.of("UTC")));
        }

        @Test
        @DisplayName("null start returns false")
        void nullStart_returnsFalse() throws Exception {
            DateRange range = range("2025-06-01", "2025-06-30");
            assertThat(invoke(null, null, range)).isFalse();
        }

        @Test
        @DisplayName("event fully within range returns true")
        void eventWithinRange_returnsTrue() throws Exception {
            DateRange range = range("2025-06-01", "2025-06-30");
            Date start = Date.from(ZonedDateTime.parse("2025-06-10T10:00:00Z").toInstant());
            Date end = Date.from(ZonedDateTime.parse("2025-06-10T11:00:00Z").toInstant());
            assertThat(invoke(start, end, range)).isTrue();
        }

        @Test
        @DisplayName("event before range returns false")
        void eventBeforeRange_returnsFalse() throws Exception {
            DateRange range = range("2025-06-01", "2025-06-30");
            Date start = Date.from(ZonedDateTime.parse("2025-05-10T10:00:00Z").toInstant());
            Date end = Date.from(ZonedDateTime.parse("2025-05-10T11:00:00Z").toInstant());
            assertThat(invoke(start, end, range)).isFalse();
        }

        @Test
        @DisplayName("event after range returns false")
        void eventAfterRange_returnsFalse() throws Exception {
            DateRange range = range("2025-06-01", "2025-06-30");
            Date start = Date.from(ZonedDateTime.parse("2025-07-05T10:00:00Z").toInstant());
            Date end = Date.from(ZonedDateTime.parse("2025-07-05T11:00:00Z").toInstant());
            assertThat(invoke(start, end, range)).isFalse();
        }

        @Test
        @DisplayName("event overlapping start of range returns true")
        void eventOverlappingRangeStart_returnsTrue() throws Exception {
            DateRange range = range("2025-06-01", "2025-06-30");
            Date start = Date.from(ZonedDateTime.parse("2025-05-30T22:00:00Z").toInstant());
            Date end = Date.from(ZonedDateTime.parse("2025-06-01T10:00:00Z").toInstant());
            assertThat(invoke(start, end, range)).isTrue();
        }

        @Test
        @DisplayName("null end date treated as instant event at start time")
        void nullEnd_treatedAsInstant() throws Exception {
            DateRange range = range("2025-06-01", "2025-06-30");
            Date start = Date.from(ZonedDateTime.parse("2025-06-15T09:00:00Z").toInstant());
            assertThat(invoke(start, null, range)).isTrue();
        }
    }

    // ── nextOccurrence ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("nextOccurrence")
    class NextOccurrenceTests {

        private Method nextOccurrence;

        @BeforeEach
        void setUp() throws Exception {
            nextOccurrence = OutlookCalendarService.class
                    .getDeclaredMethod("nextOccurrence", ZonedDateTime.class, short.class, int.class);
            nextOccurrence.setAccessible(true);
        }

        private ZonedDateTime invoke(ZonedDateTime current, short freq, int period) throws Exception {
            return (ZonedDateTime) nextOccurrence.invoke(service, current, freq, period);
        }

        private final ZonedDateTime base = ZonedDateTime.parse("2025-01-01T10:00:00Z");

        @Test
        @DisplayName("daily recurrence advances by period days")
        void dailyRecurrence_advancesByDays() throws Exception {
            ZonedDateTime next = invoke(base, (short) 0x200A, 3);
            assertThat(next).isEqualTo(base.plusDays(3));
        }

        @Test
        @DisplayName("weekly recurrence advances by period weeks")
        void weeklyRecurrence_advancesByWeeks() throws Exception {
            ZonedDateTime next = invoke(base, (short) 0x200B, 2);
            assertThat(next).isEqualTo(base.plusWeeks(2));
        }

        @Test
        @DisplayName("monthly recurrence (0x200C) advances by period months")
        void monthlyRecurrence200C_advancesByMonths() throws Exception {
            ZonedDateTime next = invoke(base, (short) 0x200C, 1);
            assertThat(next).isEqualTo(base.plusMonths(1));
        }

        @Test
        @DisplayName("monthly recurrence (0x200D) advances by period months")
        void monthlyRecurrence200D_advancesByMonths() throws Exception {
            ZonedDateTime next = invoke(base, (short) 0x200D, 3);
            assertThat(next).isEqualTo(base.plusMonths(3));
        }

        @Test
        @DisplayName("yearly recurrence (0x200E) advances by period months")
        void yearlyRecurrence_advancesByMonths() throws Exception {
            ZonedDateTime next = invoke(base, (short) 0x200E, 12);
            assertThat(next).isEqualTo(base.plusMonths(12));
        }

        @Test
        @DisplayName("unknown freq defaults to one day advance")
        void unknownFreq_defaultsToOneDay() throws Exception {
            ZonedDateTime next = invoke(base, (short) 0x9999, 5);
            assertThat(next).isEqualTo(base.plusDays(1));
        }

        @Test
        @DisplayName("zero period treated as 1")
        void zeroPeriod_treatedAsOne() throws Exception {
            ZonedDateTime next = invoke(base, (short) 0x200A, 0);
            assertThat(next).isEqualTo(base.plusDays(1));
        }
    }

    // ── validateProfilePath ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateProfilePath")
    class ValidateProfilePathTests {

        @Test
        @DisplayName("null path throws IllegalArgumentException")
        void nullPath_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.listCalendars(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not configured");
        }

        @Test
        @DisplayName("blank path throws IllegalArgumentException")
        void blankPath_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.listCalendars("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not configured");
        }

        @Test
        @DisplayName("nonexistent path throws IllegalArgumentException")
        void nonexistentPath_throwsIllegalArgument() {
            assertThatThrownBy(() -> service.listCalendars("/does/not/exist.pst"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not exist");
        }
    }

    // ── resolveZone ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveZone")
    class ResolveZoneTests {

        @Test
        @DisplayName("null PSTTimeZone returns UTC")
        void null_returnsUtc() {
            assertThat(service.resolveZone(null)).isEqualTo(ZoneId.of("UTC"));
        }

        @Test
        @DisplayName("PSTTimeZone with valid SimpleTimeZone returns its zone")
        void validSimpleTimeZone_returnsZone() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            SimpleTimeZone stz = new SimpleTimeZone(3600000, "Europe/Paris");
            org.mockito.Mockito.when(pstTz.getSimpleTimeZone()).thenReturn(stz);
            ZoneId result = service.resolveZone(pstTz);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("null SimpleTimeZone with valid IANA name falls through to name lookup")
        void nullSimpleTimeZone_validIanaName_returnsZone() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            org.mockito.Mockito.when(pstTz.getSimpleTimeZone()).thenReturn(null);
            org.mockito.Mockito.when(pstTz.getName()).thenReturn("Europe/London");
            ZoneId result = service.resolveZone(pstTz);
            assertThat(result).isEqualTo(ZoneId.of("Europe/London"));
        }

        @Test
        @DisplayName("null SimpleTimeZone with Windows TZ name uses WINDOWS_TZ_MAP")
        void nullSimpleTimeZone_windowsTzName_returnsMapped() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            org.mockito.Mockito.when(pstTz.getSimpleTimeZone()).thenReturn(null);
            org.mockito.Mockito.when(pstTz.getName()).thenReturn("Central European Standard Time");
            ZoneId result = service.resolveZone(pstTz);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("null SimpleTimeZone and blank name falls back to UTC")
        void nullSimpleTimeZone_blankName_returnsUtc() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            org.mockito.Mockito.when(pstTz.getSimpleTimeZone()).thenReturn(null);
            org.mockito.Mockito.when(pstTz.getName()).thenReturn("");
            ZoneId result = service.resolveZone(pstTz);
            assertThat(result).isEqualTo(ZoneId.of("UTC"));
        }
    }

    // ── resolveIanaZone ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resolveIanaZone")
    class ResolveIanaZoneTests {

        @Test
        @DisplayName("null PSTTimeZone returns UTC")
        void null_returnsUtc() {
            assertThat(service.resolveIanaZone(null)).isEqualTo(ZoneId.of("UTC"));
        }

        @Test
        @DisplayName("valid IANA name returns zone")
        void validIanaName_returnsZone() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            org.mockito.Mockito.when(pstTz.getName()).thenReturn("America/New_York");
            ZoneId result = service.resolveIanaZone(pstTz);
            assertThat(result).isEqualTo(ZoneId.of("America/New_York"));
        }

        @Test
        @DisplayName("Windows TZ name mapped to IANA via map")
        void windowsTzName_returnsMapped() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            org.mockito.Mockito.when(pstTz.getName()).thenReturn("Eastern Standard Time");
            ZoneId result = service.resolveIanaZone(pstTz);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("blank name with valid SimpleTimeZone falls back to SimpleTimeZone")
        void blankName_validSimpleTimeZone_returnsZone() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            org.mockito.Mockito.when(pstTz.getName()).thenReturn(null);
            org.mockito.Mockito.when(pstTz.getSimpleTimeZone()).thenReturn(
                    new SimpleTimeZone(-18000000, "EST"));
            ZoneId result = service.resolveIanaZone(pstTz);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("blank name and null SimpleTimeZone returns UTC")
        void blankName_nullSimpleTimeZone_returnsUtc() {
            PSTTimeZone pstTz = org.mockito.Mockito.mock(PSTTimeZone.class);
            org.mockito.Mockito.when(pstTz.getName()).thenReturn(null);
            org.mockito.Mockito.when(pstTz.getSimpleTimeZone()).thenReturn(null);
            ZoneId result = service.resolveIanaZone(pstTz);
            assertThat(result).isEqualTo(ZoneId.of("UTC"));
        }
    }

    // ── localMinToZdt ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("localMinToZdt")
    class LocalMinToZdtTests {

        @Test
        @DisplayName("converts 1601-epoch minutes to ZonedDateTime in given zone")
        void convertsMinutesToZdt() {
            // 194074560 minutes from 1601-01-01 = Unix epoch 1970-01-01T00:00:00Z
            long epochMin = 194074560L;
            ZonedDateTime result = service.localMinToZdt(epochMin, ZoneId.of("UTC"));
            assertThat(result.toLocalDateTime()).isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
        }

        @Test
        @DisplayName("applies timezone to the converted LocalDateTime")
        void appliesTimezone() {
            long epochMin = 194074560L;
            ZoneId zone = ZoneId.of("Europe/Warsaw");
            ZonedDateTime result = service.localMinToZdt(epochMin, zone);
            assertThat(result.getZone()).isEqualTo(zone);
        }
    }
}
