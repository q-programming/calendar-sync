package pl.qprogramming.calendarsync.service.google;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
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
import pl.qprogramming.calendarsync.service.ProfileService;
import pl.qprogramming.calendarsync.service.google.GoogleCalendarService;
import pl.qprogramming.calendarsync.service.google.GoogleEvent;
import pl.qprogramming.calendarsync.service.outlook.OutlookEvent;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for GoogleCalendarService private conversion methods via reflection.
 * These methods contain pure data-transformation logic that does not require
 * an active Google OAuth connection.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GoogleCalendarService — private conversion methods")
class GoogleCalendarServicePrivateMethodTest {

    @Mock private OAuth2AuthorizedClientService authorizedClientService;
    @Mock private ProfileService profileService;

    @InjectMocks
    private GoogleCalendarService service;

    // ── toGoogleApiEvent ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toGoogleApiEvent")
    class ToGoogleApiEventTests {

        private Method toGoogleApiEvent;

        @BeforeEach
        void setUp() throws Exception {
            toGoogleApiEvent = GoogleCalendarService.class
                    .getDeclaredMethod("toGoogleApiEvent", OutlookEvent.class, boolean.class);
            toGoogleApiEvent.setAccessible(true);
        }

        private OutlookEvent makeEvent(String id, String subject, String body, String location,
                                       ZonedDateTime start, ZonedDateTime end, boolean allDay, int colorIndex) {
            return new OutlookEvent(id, subject, body, location, start, end, allDay, colorIndex);
        }

        @Test
        @DisplayName("timed event maps summary, description, location and dateTime fields")
        void timedEvent_mapsAllFields() throws Exception {
            ZonedDateTime start = ZonedDateTime.of(2025, 6, 1, 9, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
            ZonedDateTime end = ZonedDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneId.of("Europe/Warsaw"));
            var outlook = makeEvent("ol-1", "Meeting", "Discuss project", "Room A", start, end, false, 0);

            Event result = (Event) toGoogleApiEvent.invoke(service, outlook, false);

            assertThat(result.getSummary()).isEqualTo("Meeting");
            assertThat(result.getDescription()).isEqualTo("Discuss project");
            assertThat(result.getLocation()).isEqualTo("Room A");
            assertThat(result.getStart().getDateTime()).isNotNull();
            assertThat(result.getEnd().getDateTime()).isNotNull();
            assertThat(result.getStart().getTimeZone()).isEqualTo("Europe/Warsaw");
        }

        @Test
        @DisplayName("all-day event uses date fields not dateTime")
        void allDayEvent_usesDateNotDateTime() throws Exception {
            ZonedDateTime start = ZonedDateTime.of(2025, 7, 4, 0, 0, 0, 0, ZoneId.of("UTC"));
            ZonedDateTime end = ZonedDateTime.of(2025, 7, 5, 0, 0, 0, 0, ZoneId.of("UTC"));
            var outlook = makeEvent("ol-2", "Holiday", "", null, start, end, true, 0);

            Event result = (Event) toGoogleApiEvent.invoke(service, outlook, false);

            assertThat(result.getStart().getDate()).isNotNull();
            assertThat(result.getStart().getDateTime()).isNull();
            assertThat(result.getEnd().getDate()).isNotNull();
        }

        @Test
        @DisplayName("extended properties contain sync marker and outlookId")
        void extendedProperties_containSyncMarker() throws Exception {
            ZonedDateTime start = ZonedDateTime.of(2025, 1, 1, 8, 0, 0, 0, ZoneId.of("UTC"));
            var outlook = makeEvent("my-ol-id", "Test", "", null, start, start.plusHours(1), false, 0);

            Event result = (Event) toGoogleApiEvent.invoke(service, outlook, false);

            Map<String, String> priv = result.getExtendedProperties().getPrivate();
            assertThat(priv).containsEntry("source", "outlook-sync");
            assertThat(priv).containsEntry("outlookId", "my-ol-id");
        }

        @Test
        @DisplayName("color is set when syncColorLabels is true and colorIndex is valid")
        void color_setWhenEnabled() throws Exception {
            ZonedDateTime start = ZonedDateTime.of(2025, 3, 15, 10, 0, 0, 0, ZoneId.of("UTC"));
            var outlook = makeEvent("ol-3", "Colored", "", null, start, start.plusHours(1), false, 1); // colorIndex=1 (Tomato/Red)

            Event result = (Event) toGoogleApiEvent.invoke(service, outlook, true);

            assertThat(result.getColorId()).isNotNull();
        }

        @Test
        @DisplayName("color is not set when syncColorLabels is false")
        void color_notSetWhenDisabled() throws Exception {
            ZonedDateTime start = ZonedDateTime.of(2025, 3, 15, 10, 0, 0, 0, ZoneId.of("UTC"));
            var outlook = makeEvent("ol-4", "Colored", "", null, start, start.plusHours(1), false, 1);

            Event result = (Event) toGoogleApiEvent.invoke(service, outlook, false);

            assertThat(result.getColorId()).isNull();
        }

        @Test
        @DisplayName("null location does not cause NPE")
        void nullLocation_handled() throws Exception {
            ZonedDateTime start = ZonedDateTime.of(2025, 4, 1, 9, 0, 0, 0, ZoneId.of("UTC"));
            var outlook = makeEvent("ol-5", "NoLoc", null, null, start, start.plusHours(1), false, 0);

            Event result = (Event) toGoogleApiEvent.invoke(service, outlook, false);

            assertThat(result.getLocation()).isNull();
        }
    }

    // ── toGoogleEvent ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toGoogleEvent")
    class ToGoogleEventTests {

        private Method toGoogleEvent;

        @BeforeEach
        void setUp() throws Exception {
            toGoogleEvent = GoogleCalendarService.class
                    .getDeclaredMethod("toGoogleEvent", Event.class);
            toGoogleEvent.setAccessible(true);
        }

        @Test
        @DisplayName("converts basic event with no extended properties")
        void basicEvent_convertsCorrectly() throws Exception {
            Event e = new Event();
            e.setId("g-id-1");
            e.setSummary("Team Meeting");
            e.setDescription("Discuss plans");
            e.setLocation("Conf Room");
            long epochMillis = Instant.parse("2025-06-01T09:00:00Z").toEpochMilli();
            e.setStart(new EventDateTime().setDateTime(new DateTime(epochMillis)).setTimeZone("UTC"));
            e.setEnd(new EventDateTime().setDateTime(new DateTime(epochMillis + 3600000L)).setTimeZone("UTC"));

            GoogleEvent result = (GoogleEvent) toGoogleEvent.invoke(service, e);

            assertThat(result.id()).isEqualTo("g-id-1");
            assertThat(result.summary()).isEqualTo("Team Meeting");
            assertThat(result.description()).isEqualTo("Discuss plans");
            assertThat(result.location()).isEqualTo("Conf Room");
            assertThat(result.outlookId()).isNull();
            assertThat(result.allDay()).isFalse();
        }

        @Test
        @DisplayName("reads outlookId from extended properties")
        void readsOutlookIdFromExtendedProperties() throws Exception {
            Event e = new Event();
            e.setId("g-id-2");
            e.setExtendedProperties(new Event.ExtendedProperties()
                    .setPrivate(Map.of("outlookId", "ol-abc-123")));
            e.setStart(new EventDateTime().setDate(new DateTime("2025-07-04")));
            e.setEnd(new EventDateTime().setDate(new DateTime("2025-07-05")));

            GoogleEvent result = (GoogleEvent) toGoogleEvent.invoke(service, e);

            assertThat(result.outlookId()).isEqualTo("ol-abc-123");
            assertThat(result.allDay()).isTrue();
        }

        @Test
        @DisplayName("all-day event is detected via date field presence")
        void allDayEvent_detected() throws Exception {
            Event e = new Event();
            e.setId("g-id-3");
            e.setStart(new EventDateTime().setDate(new DateTime("2025-08-15")));
            e.setEnd(new EventDateTime().setDate(new DateTime("2025-08-16")));

            GoogleEvent result = (GoogleEvent) toGoogleEvent.invoke(service, e);

            assertThat(result.allDay()).isTrue();
        }
    }

    // ── parseDateTime ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseDateTime")
    class ParseDateTimeTests {

        private Method parseDateTime;

        @BeforeEach
        void setUp() throws Exception {
            parseDateTime = GoogleCalendarService.class
                    .getDeclaredMethod("parseDateTime", EventDateTime.class);
            parseDateTime.setAccessible(true);
        }

        @Test
        @DisplayName("null EventDateTime returns null")
        void nullInput_returnsNull() throws Exception {
            ZonedDateTime result = (ZonedDateTime) parseDateTime.invoke(service, (Object) null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("dateTime field parsed with zone")
        void dateTimeField_parsedWithZone() throws Exception {
            long epochMillis = Instant.parse("2025-06-01T14:00:00Z").toEpochMilli();
            EventDateTime edt = new EventDateTime()
                    .setDateTime(new DateTime(epochMillis))
                    .setTimeZone("Europe/Warsaw");

            ZonedDateTime result = (ZonedDateTime) parseDateTime.invoke(service, edt);

            assertThat(result).isNotNull();
            assertThat(result.getZone().getId()).isEqualTo("Europe/Warsaw");
        }

        @Test
        @DisplayName("dateTime field without zone defaults to UTC")
        void dateTimeField_noZone_defaultsToUTC() throws Exception {
            long epochMillis = Instant.parse("2025-01-15T10:00:00Z").toEpochMilli();
            EventDateTime edt = new EventDateTime().setDateTime(new DateTime(epochMillis));

            ZonedDateTime result = (ZonedDateTime) parseDateTime.invoke(service, edt);

            assertThat(result).isNotNull();
            assertThat(result.getZone().getId()).isEqualTo("UTC");
        }

        @Test
        @DisplayName("date field (all-day) parsed as UTC midnight")
        void dateField_parsedAsUtcMidnight() throws Exception {
            EventDateTime edt = new EventDateTime().setDate(new DateTime("2025-12-25"));

            ZonedDateTime result = (ZonedDateTime) parseDateTime.invoke(service, edt);

            assertThat(result).isNotNull();
            assertThat(result.getHour()).isEqualTo(0);
            assertThat(result.getMinute()).isEqualTo(0);
            assertThat(result.getZone()).isEqualTo(ZoneId.of("UTC"));
        }

        @Test
        @DisplayName("empty EventDateTime (no date, no dateTime) returns null")
        void emptyEventDateTime_returnsNull() throws Exception {
            EventDateTime edt = new EventDateTime(); // neither date nor dateTime set

            ZonedDateTime result = (ZonedDateTime) parseDateTime.invoke(service, edt);

            assertThat(result).isNull();
        }
    }
}
