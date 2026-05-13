package pl.qprogramming.calendarsync.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Model records")
class ModelTest {

    // ── DateRange ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DateRange stores from and to")
    void dateRange_storesFromAndTo() {
        var from = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var to   = ZonedDateTime.of(2025, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC);
        var range = new DateRange(from, to);

        assertThat(range.from()).isEqualTo(from);
        assertThat(range.to()).isEqualTo(to);
    }

    @Test
    @DisplayName("DateRange equality by value")
    void dateRange_equality() {
        var from = ZonedDateTime.of(2025, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        var to   = ZonedDateTime.of(2025, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC);
        assertThat(new DateRange(from, to)).isEqualTo(new DateRange(from, to));
    }

    // ── CalendarRef ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("CalendarRef stores all fields")
    void calendarRef_storesFields() {
        var ref = new CalendarRef("id-1", "My Calendar", "Europe/Warsaw", "#FF0000");

        assertThat(ref.id()).isEqualTo("id-1");
        assertThat(ref.name()).isEqualTo("My Calendar");
        assertThat(ref.timeZone()).isEqualTo("Europe/Warsaw");
        assertThat(ref.color()).isEqualTo("#FF0000");
    }

    @Test
    @DisplayName("CalendarRef allows null timeZone and color")
    void calendarRef_allowsNulls() {
        var ref = new CalendarRef("id-2", "Cal", null, null);

        assertThat(ref.timeZone()).isNull();
        assertThat(ref.color()).isNull();
    }

    @Test
    @DisplayName("CalendarRef equality by value")
    void calendarRef_equality() {
        assertThat(new CalendarRef("a", "b", "UTC", null))
                .isEqualTo(new CalendarRef("a", "b", "UTC", null));
    }
}
