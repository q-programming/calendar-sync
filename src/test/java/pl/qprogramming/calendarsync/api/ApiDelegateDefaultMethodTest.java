package pl.qprogramming.calendarsync.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import pl.qprogramming.calendarsync.dto.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that exercise the default method implementations provided by each generated
 * API delegate interface. These default methods return {@code NOT_IMPLEMENTED} and
 * represent the fallback behaviour when no custom delegate is registered.
 */
@DisplayName("Generated API delegate — default methods")
class ApiDelegateDefaultMethodTest {

    // ── HealthzApiDelegate ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("HealthzApiDelegate")
    class HealthzTests {

        private final HealthzApiDelegate delegate = new HealthzApiDelegate() {};

        @Test
        @DisplayName("getRequest() returns empty Optional")
        void getRequest_returnsEmpty() {
            assertThat(delegate.getRequest()).isEqualTo(Optional.empty());
        }

        @Test
        @DisplayName("healthCheck() returns 501 NOT_IMPLEMENTED")
        void healthCheck_returns501() {
            assertThat(delegate.healthCheck().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // ── LogsApiDelegate ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("LogsApiDelegate")
    class LogsTests {

        private final LogsApiDelegate delegate = new LogsApiDelegate() {};

        @Test
        @DisplayName("getRequest() returns empty Optional")
        void getRequest_returnsEmpty() {
            assertThat(delegate.getRequest()).isEqualTo(Optional.empty());
        }

        @Test
        @DisplayName("getLogDetails() returns 501 NOT_IMPLEMENTED")
        void getLogDetails_returns501() {
            assertThat(delegate.getLogDetails("run-1").getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("getLogs() returns 501 NOT_IMPLEMENTED")
        void getLogs_returns501() {
            assertThat(delegate.getLogs(0, 20, null).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // ── ProfileApiDelegate ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("ProfileApiDelegate")
    class ProfileTests {

        private final ProfileApiDelegate delegate = new ProfileApiDelegate() {};

        @Test
        @DisplayName("getRequest() returns empty Optional")
        void getRequest_returnsEmpty() {
            assertThat(delegate.getRequest()).isEqualTo(Optional.empty());
        }

        @Test
        @DisplayName("connectOutlook() returns 501 NOT_IMPLEMENTED")
        void connectOutlook_returns501() {
            assertThat(delegate.connectOutlook(new OutlookConnection("/path.pst")).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("disconnectGoogle() returns 501 NOT_IMPLEMENTED")
        void disconnectGoogle_returns501() {
            assertThat(delegate.disconnectGoogle().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("disconnectOutlook() returns 501 NOT_IMPLEMENTED")
        void disconnectOutlook_returns501() {
            assertThat(delegate.disconnectOutlook().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("getGoogleCalendars() returns 501 NOT_IMPLEMENTED")
        void getGoogleCalendars_returns501() {
            assertThat(delegate.getGoogleCalendars().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("getOutlookCalendars() returns 501 NOT_IMPLEMENTED")
        void getOutlookCalendars_returns501() {
            assertThat(delegate.getOutlookCalendars().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("getProfile() returns 501 NOT_IMPLEMENTED")
        void getProfile_returns501() {
            assertThat(delegate.getProfile().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("setGoogleCalendar() returns 501 NOT_IMPLEMENTED")
        void setGoogleCalendar_returns501() {
            assertThat(delegate.setGoogleCalendar(new CalendarSelection("cal-1")).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("setOutlookCalendar() returns 501 NOT_IMPLEMENTED")
        void setOutlookCalendar_returns501() {
            assertThat(delegate.setOutlookCalendar(new CalendarSelection("cal-2")).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // ── SettingsApiDelegate ────────────────────────────────────────────────────

    @Nested
    @DisplayName("SettingsApiDelegate")
    class SettingsTests {

        private final SettingsApiDelegate delegate = new SettingsApiDelegate() {};

        @Test
        @DisplayName("getRequest() returns empty Optional")
        void getRequest_returnsEmpty() {
            assertThat(delegate.getRequest()).isEqualTo(Optional.empty());
        }

        @Test
        @DisplayName("getSettings() returns 501 NOT_IMPLEMENTED")
        void getSettings_returns501() {
            assertThat(delegate.getSettings().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }

        @Test
        @DisplayName("updateSettings() returns 501 NOT_IMPLEMENTED")
        void updateSettings_returns501() {
            assertThat(delegate.updateSettings(new SyncSettings(30, 7, 30, false, false)).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }
    }

    // ── SyncApiDelegate ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SyncApiDelegate")
    class SyncTests {

        private final SyncApiDelegate delegate = new SyncApiDelegate() {};

        @Test
        @DisplayName("getRequest() returns empty Optional")
        void getRequest_returnsEmpty() {
            assertThat(delegate.getRequest()).isEqualTo(Optional.empty());
        }

        @Test
        @DisplayName("triggerSync() returns 501 NOT_IMPLEMENTED")
        void triggerSync_returns501() {
            assertThat(delegate.triggerSync().getStatusCode())
                    .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        }
    }
}
