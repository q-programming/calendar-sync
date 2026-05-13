package pl.qprogramming.calendarsync.service.google;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.model.DateRange;
import pl.qprogramming.calendarsync.service.ProfileService;
import pl.qprogramming.calendarsync.service.outlook.OutlookEvent;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GoogleCalendarService")
class GoogleCalendarServiceTest {

    @Mock OAuth2AuthorizedClientService authorizedClientService;
    @Mock ProfileService profileService;

    @InjectMocks GoogleCalendarService service;

    private ProfileEntity profileWithPrincipal(String name) {
        var p = new ProfileEntity();
        p.setGooglePrincipalName(name);
        return p;
    }

    @BeforeEach
    void stubProfile() {
        when(profileService.getOrCreate()).thenReturn(profileWithPrincipal(null));
    }

    // ── batchWrite — empty inputs ─────────────────────────────────────────────

    @Nested
    @DisplayName("batchWrite with empty inputs")
    class BatchWriteEmpty {

        @Test
        @DisplayName("returns BatchWriteResult.empty() when all lists are empty")
        void returnsEmpty_WhenNothingToDo() {
            var result = service.batchWrite("cal-id", List.of(), Map.of(), List.of(), false);

            assertThat(result.created()).isZero();
            assertThat(result.updated()).isZero();
            assertThat(result.deleted()).isZero();
            assertThat(result.failed()).isZero();
        }

        @Test
        @DisplayName("does not call Google API when inputs are empty")
        void doesNotCallGoogle_WhenEmpty() {
            // profileService is stubbed to return null principal — if buildClient() were called it would throw
            // The fact that no exception is thrown proves buildClient() was NOT called
            var result = service.batchWrite("cal-id", List.of(), Map.of(), List.of(), true);

            assertThat(result).isEqualTo(BatchWriteResult.empty());
        }
    }

    // ── listCalendars — no Google connection ──────────────────────────────────

    @Nested
    @DisplayName("listCalendars — no Google connection")
    class ListCalendarsNoConnection {

        @Test
        @DisplayName("throws RuntimeException wrapping IllegalStateException when no principal")
        void throwsRuntimeException_WhenNoPrincipal() {
            when(profileService.getOrCreate()).thenReturn(profileWithPrincipal(null));

            assertThatThrownBy(() -> service.listCalendars())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to list Google calendars");
        }

        @Test
        @DisplayName("throws RuntimeException when authorized client is null")
        void throwsRuntimeException_WhenClientNull() {
            when(profileService.getOrCreate()).thenReturn(profileWithPrincipal("user@example.com"));
            when(authorizedClientService.loadAuthorizedClient("google", "user@example.com"))
                    .thenReturn(null);

            assertThatThrownBy(() -> service.listCalendars())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to list Google calendars");
        }
    }

    // ── readEvents — no Google connection ────────────────────────────────────

    @Nested
    @DisplayName("readEvents — no Google connection")
    class ReadEventsNoConnection {

        private final DateRange range = new DateRange(
                ZonedDateTime.now(ZoneOffset.UTC).minusDays(7),
                ZonedDateTime.now(ZoneOffset.UTC).plusDays(30));

        @Test
        @DisplayName("throws RuntimeException when no principal configured")
        void throwsRuntimeException_WhenNoPrincipal() {
            when(profileService.getOrCreate()).thenReturn(profileWithPrincipal(null));

            assertThatThrownBy(() -> service.readEvents("cal-id", range))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to read Google events");
        }

        @Test
        @DisplayName("throws RuntimeException when authorized client is null")
        void throwsRuntimeException_WhenClientNull() {
            when(profileService.getOrCreate()).thenReturn(profileWithPrincipal("user@example.com"));
            when(authorizedClientService.loadAuthorizedClient("google", "user@example.com"))
                    .thenReturn(null);

            assertThatThrownBy(() -> service.readEvents("cal-id", range))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to read Google events");
        }
    }

    // ── batchWrite — non-empty, no connection ─────────────────────────────────

    @Nested
    @DisplayName("batchWrite with events — no Google connection")
    class BatchWriteNoConnection {

        private final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        @Test
        @DisplayName("throws RuntimeException when toCreate is non-empty but no principal")
        void throwsRuntimeException_WhenNoPrincipalAndCreate() {
            var event = new OutlookEvent("id1", "Meeting", null, null,
                    now, now.plusHours(1), false, 0);
            when(profileService.getOrCreate()).thenReturn(profileWithPrincipal(null));

            assertThatThrownBy(() -> service.batchWrite("cal-id", List.of(event), Map.of(), List.of(), false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Batch write failed");
        }

        @Test
        @DisplayName("throws RuntimeException when toDeleteIds is non-empty but no principal")
        void throwsRuntimeException_WhenNoPrincipalAndDelete() {
            when(profileService.getOrCreate()).thenReturn(profileWithPrincipal(null));

            assertThatThrownBy(() -> service.batchWrite("cal-id", List.of(), Map.of(), List.of("gid-1"), false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Batch write failed");
        }
    }
}
