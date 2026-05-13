package pl.qprogramming.calendarsync.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pl.qprogramming.calendarsync.dto.CalendarSelection;
import pl.qprogramming.calendarsync.dto.OutlookConnection;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.model.CalendarRef;
import pl.qprogramming.calendarsync.service.ProfileService;
import pl.qprogramming.calendarsync.service.SyncService;
import pl.qprogramming.calendarsync.service.google.GoogleCalendarService;
import pl.qprogramming.calendarsync.service.outlook.OutlookCalendarService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileController")
class ProfileControllerTest {

    @Mock ProfileService profileService;
    @Mock OutlookCalendarService outlookService;
    @Mock GoogleCalendarService googleService;
    @Mock SyncService syncService;

    @InjectMocks
    ProfileController controller;

    // ── getProfile ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfile")
    class GetProfile {

        @Test
        @DisplayName("returns 200 with mapped DTO fields")
        void returns200WithMappedFields() {
            var entity = new ProfileEntity();
            entity.setOutlookProfilePath("/some/path.pst");
            entity.setOutlookCalendarId("oc-id");
            entity.setOutlookCalendarName("Outlook Cal");
            entity.setGoogleCalendarId("gc-id");
            entity.setGoogleCalendarName("Google Cal");
            entity.setGooglePrincipalName("user@example.com");

            when(profileService.getOrCreate()).thenReturn(entity);
            when(profileService.isGoogleConnected()).thenReturn(true);
            when(syncService.isRunning()).thenReturn(false);

            var response = controller.getProfile();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var dto = response.getBody();
            assertThat(dto).isNotNull();
            assertThat(dto.getGoogleConnected()).isTrue();
            assertThat(dto.getOutlookConnected()).isTrue();
            assertThat(dto.getSyncRunning()).isFalse();
            assertThat(dto.getOutlookProfilePath()).isEqualTo("/some/path.pst");
            assertThat(dto.getOutlookCalendarId()).isEqualTo("oc-id");
            assertThat(dto.getOutlookCalendarName()).isEqualTo("Outlook Cal");
            assertThat(dto.getGoogleCalendarId()).isEqualTo("gc-id");
            assertThat(dto.getGoogleCalendarName()).isEqualTo("Google Cal");
        }

        @Test
        @DisplayName("outlookConnected is false when outlookProfilePath is null")
        void outlookConnectedFalse_WhenPathNull() {
            var entity = new ProfileEntity(); // null path
            when(profileService.getOrCreate()).thenReturn(entity);
            when(profileService.isGoogleConnected()).thenReturn(false);
            when(syncService.isRunning()).thenReturn(false);

            var response = controller.getProfile();

            assertThat(response.getBody().getOutlookConnected()).isFalse();
        }

        @Test
        @DisplayName("syncRunning is true when sync is in progress")
        void syncRunningTrue_WhenSyncIsRunning() {
            when(profileService.getOrCreate()).thenReturn(new ProfileEntity());
            when(profileService.isGoogleConnected()).thenReturn(false);
            when(syncService.isRunning()).thenReturn(true);

            var response = controller.getProfile();

            assertThat(response.getBody().getSyncRunning()).isTrue();
        }
    }

    // ── connectOutlook ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("connectOutlook")
    class ConnectOutlook {

        @Test
        @DisplayName("returns 204 when profile path is valid")
        void returns204_WhenPathValid() {
            var connection = new OutlookConnection();
            connection.setProfilePath("/valid/path.pst");
            doNothing().when(profileService).connectOutlook(anyString());

            var response = controller.connectOutlook(connection);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("returns 400 when path is invalid (IAE thrown)")
        void returns400_WhenPathInvalid() {
            var connection = new OutlookConnection();
            connection.setProfilePath("/bad/path.pst");
            doThrow(new IllegalArgumentException("Not found")).when(profileService).connectOutlook(anyString());

            var response = controller.connectOutlook(connection);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── getOutlookCalendars ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getOutlookCalendars")
    class GetOutlookCalendars {

        @Test
        @DisplayName("returns 400 when outlookProfilePath is null")
        void returns400_WhenNoPath() {
            when(profileService.getOrCreate()).thenReturn(new ProfileEntity());

            var response = controller.getOutlookCalendars();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("returns 200 with mapped calendar list")
        void returns200WithCalendars() {
            var entity = new ProfileEntity();
            entity.setOutlookProfilePath("/path.pst");
            when(profileService.getOrCreate()).thenReturn(entity);

            var ref = new CalendarRef("cal-id", "My Cal", "UTC", null);
            when(outlookService.listCalendars("/path.pst")).thenReturn(List.of(ref));

            var response = controller.getOutlookCalendars();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getId()).isEqualTo("cal-id");
            assertThat(response.getBody().get(0).getName()).isEqualTo("My Cal");
        }

        @Test
        @DisplayName("returns 400 when listCalendars throws IAE")
        void returns400_WhenListCalendarsThrowsIAE() {
            var entity = new ProfileEntity();
            entity.setOutlookProfilePath("/path.pst");
            when(profileService.getOrCreate()).thenReturn(entity);
            when(outlookService.listCalendars("/path.pst"))
                    .thenThrow(new IllegalArgumentException("bad PST"));

            var response = controller.getOutlookCalendars();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ── setOutlookCalendar ────────────────────────────────────────────────────

    @Nested
    @DisplayName("setOutlookCalendar")
    class SetOutlookCalendar {

        @Test
        @DisplayName("returns 204 and saves calendar selection")
        void returns204AndSavesCalendar() {
            var selection = new CalendarSelection();
            selection.setCalendarId("oc-id");

            var entity = new ProfileEntity();
            entity.setOutlookProfilePath("/path.pst");
            when(profileService.getOrCreate()).thenReturn(entity);

            var ref = new CalendarRef("oc-id", "My Outlook Cal", "CET", null);
            when(outlookService.listCalendars("/path.pst")).thenReturn(List.of(ref));

            doNothing().when(profileService).setOutlookCalendar(anyString(), anyString());

            var response = controller.setOutlookCalendar(selection);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(profileService).setOutlookCalendar("oc-id", "My Outlook Cal");
        }
    }

    // ── getGoogleCalendars ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getGoogleCalendars")
    class GetGoogleCalendars {

        @Test
        @DisplayName("returns 401 when not Google connected")
        void returns401_WhenNotConnected() {
            when(profileService.isGoogleConnected()).thenReturn(false);

            var response = controller.getGoogleCalendars();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("returns 200 with mapped Google calendar list")
        void returns200WithCalendars() {
            when(profileService.isGoogleConnected()).thenReturn(true);
            var ref = new CalendarRef("gcal-id", "Google Primary", "UTC", "#000");
            when(googleService.listCalendars()).thenReturn(List.of(ref));

            var response = controller.getGoogleCalendars();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().get(0).getId()).isEqualTo("gcal-id");
        }

        @Test
        @DisplayName("returns 500 when listCalendars throws")
        void returns500_WhenExceptionThrown() {
            when(profileService.isGoogleConnected()).thenReturn(true);
            when(googleService.listCalendars()).thenThrow(new RuntimeException("network error"));

            var response = controller.getGoogleCalendars();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ── setGoogleCalendar ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("setGoogleCalendar")
    class SetGoogleCalendar {

        @Test
        @DisplayName("returns 204 and saves selection")
        void returns204AndSavesSelection() {
            var selection = new CalendarSelection();
            selection.setCalendarId("gcal-id");

            var ref = new CalendarRef("gcal-id", "Primary Calendar", "UTC", null);
            when(googleService.listCalendars()).thenReturn(List.of(ref));
            doNothing().when(profileService).setGoogleCalendar(anyString(), anyString());

            var response = controller.setGoogleCalendar(selection);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(profileService).setGoogleCalendar("gcal-id", "Primary Calendar");
        }
    }

    // ── disconnectGoogle ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("disconnectGoogle")
    class DisconnectGoogle {

        @Test
        @DisplayName("returns 204 and calls profileService.disconnectGoogle")
        void returns204AndDisconnects() {
            doNothing().when(profileService).disconnectGoogle();

            var response = controller.disconnectGoogle();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(profileService).disconnectGoogle();
        }
    }

    // ── disconnectOutlook ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("disconnectOutlook")
    class DisconnectOutlook {

        @Test
        @DisplayName("returns 204 and calls profileService.disconnectOutlook")
        void returns204AndDisconnects() {
            doNothing().when(profileService).disconnectOutlook();

            var response = controller.disconnectOutlook();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(profileService).disconnectOutlook();
        }
    }
}
