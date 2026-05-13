package pl.qprogramming.calendarsync.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.NativeWebRequest;
import pl.qprogramming.calendarsync.dto.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests that exercise the delegate default methods when an {@code Accept: application/json}
 * request IS present — this covers the lambda body inside {@code getRequest().ifPresent(...)}.
 * Also tests {@link ApiUtil#setExampleResponse}.
 */
@DisplayName("Generated API delegate — default methods with Accept header")
class ApiDelegateWithRequestTest {

    private NativeWebRequest webRequest;
    private HttpServletResponse httpResponse;
    private StringWriter responseBody;

    @BeforeEach
    void setUpRequest() throws Exception {
        httpResponse = mock(HttpServletResponse.class);
        responseBody = new StringWriter();
        when(httpResponse.getWriter()).thenReturn(new PrintWriter(responseBody));
        webRequest = mock(NativeWebRequest.class);
        when(webRequest.getHeader("Accept")).thenReturn("application/json");
        when(webRequest.getNativeResponse(HttpServletResponse.class)).thenReturn(httpResponse);
    }

    // ── HealthzApiDelegate ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("HealthzApiDelegate — with Accept: application/json")
    class HealthzTests {

        private HealthzApiDelegate delegate;

        @BeforeEach
        void setUp() {
            NativeWebRequest req = webRequest;
            delegate = new HealthzApiDelegate() {
                @Override public Optional<NativeWebRequest> getRequest() { return Optional.of(req); }
            };
        }

        @Test
        @DisplayName("healthCheck() executes example response and returns 501")
        void healthCheck_executesExampleAndReturns501() throws Exception {
            var response = delegate.healthCheck();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse).addHeader(eq("Content-Type"), eq("application/json"));
        }
    }

    // ── LogsApiDelegate ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("LogsApiDelegate — with Accept: application/json")
    class LogsTests {

        private LogsApiDelegate delegate;

        @BeforeEach
        void setUp() {
            NativeWebRequest req = webRequest;
            delegate = new LogsApiDelegate() {
                @Override public Optional<NativeWebRequest> getRequest() { return Optional.of(req); }
            };
        }

        @Test
        @DisplayName("getLogDetails() executes example response and returns 501")
        void getLogDetails_executesExampleAndReturns501() throws Exception {
            var response = delegate.getLogDetails("run-id");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse).addHeader(eq("Content-Type"), eq("application/json"));
        }

        @Test
        @DisplayName("getLogs() executes example response and returns 501")
        void getLogs_executesExampleAndReturns501() throws Exception {
            var response = delegate.getLogs(0, 20, null);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse, atLeastOnce()).addHeader(anyString(), anyString());
        }
    }

    // ── ProfileApiDelegate ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("ProfileApiDelegate — with Accept: application/json")
    class ProfileTests {

        private ProfileApiDelegate delegate;

        @BeforeEach
        void setUp() {
            NativeWebRequest req = webRequest;
            delegate = new ProfileApiDelegate() {
                @Override public Optional<NativeWebRequest> getRequest() { return Optional.of(req); }
            };
        }

        @Test
        @DisplayName("getGoogleCalendars() executes example response")
        void getGoogleCalendars_executesExample() throws Exception {
            var response = delegate.getGoogleCalendars();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse).addHeader(eq("Content-Type"), eq("application/json"));
        }

        @Test
        @DisplayName("getOutlookCalendars() executes example response")
        void getOutlookCalendars_executesExample() throws Exception {
            var response = delegate.getOutlookCalendars();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse, atLeastOnce()).addHeader(anyString(), anyString());
        }

        @Test
        @DisplayName("getProfile() executes example response")
        void getProfile_executesExample() throws Exception {
            var response = delegate.getProfile();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse, atLeastOnce()).addHeader(anyString(), anyString());
        }
    }

    // ── SettingsApiDelegate ────────────────────────────────────────────────────

    @Nested
    @DisplayName("SettingsApiDelegate — with Accept: application/json")
    class SettingsTests {

        private SettingsApiDelegate delegate;

        @BeforeEach
        void setUp() {
            NativeWebRequest req = webRequest;
            delegate = new SettingsApiDelegate() {
                @Override public Optional<NativeWebRequest> getRequest() { return Optional.of(req); }
            };
        }

        @Test
        @DisplayName("getSettings() executes example response and returns 501")
        void getSettings_executesExampleAndReturns501() throws Exception {
            var response = delegate.getSettings();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse).addHeader(eq("Content-Type"), eq("application/json"));
        }
    }

    // ── SyncApiDelegate ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SyncApiDelegate — with Accept: application/json")
    class SyncTests {

        private SyncApiDelegate delegate;

        @BeforeEach
        void setUp() {
            NativeWebRequest req = webRequest;
            delegate = new SyncApiDelegate() {
                @Override public Optional<NativeWebRequest> getRequest() { return Optional.of(req); }
            };
        }

        @Test
        @DisplayName("triggerSync() executes example response and returns 501")
        void triggerSync_executesExampleAndReturns501() throws Exception {
            var response = delegate.triggerSync();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
            verify(httpResponse).addHeader(eq("Content-Type"), eq("application/json"));
        }
    }

    // ── ApiUtil ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ApiUtil")
    class ApiUtilTests {

        @Test
        @DisplayName("setExampleResponse writes to the response writer")
        void setExampleResponse_writesToWriter() throws Exception {
            StringWriter sw = new StringWriter();
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getWriter()).thenReturn(new PrintWriter(sw));
            NativeWebRequest req = mock(NativeWebRequest.class);
            when(req.getNativeResponse(HttpServletResponse.class)).thenReturn(response);

            ApiUtil.setExampleResponse(req, "application/json", "{\"key\":\"value\"}");

            verify(response).setCharacterEncoding("UTF-8");
            verify(response).addHeader("Content-Type", "application/json");
            assertThat(sw.toString()).contains("{\"key\":\"value\"}");
        }

        @Test
        @DisplayName("setExampleResponse wraps IOException as RuntimeException")
        void setExampleResponse_wrapsIOException() throws Exception {
            HttpServletResponse response = mock(HttpServletResponse.class);
            when(response.getWriter()).thenThrow(new java.io.IOException("disk full"));
            NativeWebRequest req = mock(NativeWebRequest.class);
            when(req.getNativeResponse(HttpServletResponse.class)).thenReturn(response);

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> ApiUtil.setExampleResponse(req, "application/json", "{}"))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(java.io.IOException.class);
        }

        @Test
        @DisplayName("ApiUtil can be instantiated (utility class)")
        void apiUtil_canBeInstantiated() {
            assertThat(new ApiUtil()).isNotNull();
        }
    }
}
