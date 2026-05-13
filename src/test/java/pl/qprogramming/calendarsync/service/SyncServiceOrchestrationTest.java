package pl.qprogramming.calendarsync.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncRunStatus;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.model.CalendarRef;
import pl.qprogramming.calendarsync.service.google.BatchWriteResult;
import pl.qprogramming.calendarsync.service.google.GoogleCalendarService;
import pl.qprogramming.calendarsync.service.google.GoogleEvent;
import pl.qprogramming.calendarsync.service.outlook.OutlookCalendarService;
import pl.qprogramming.calendarsync.service.outlook.OutlookEvent;

import java.lang.reflect.Field;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SyncService")
class SyncServiceOrchestrationTest {

    @Mock OutlookCalendarService outlookService;
    @Mock GoogleCalendarService googleService;
    @Mock ProfileService profileService;
    @Mock SettingsService settingsService;
    @Mock LogService logService;

    @InjectMocks
    SyncService syncService;

    private SyncSettingsEntity defaultSettings() {
        var s = new SyncSettingsEntity();
        s.setDaysPast(7);
        s.setDaysFuture(30);
        s.setDebugLogging(false);
        s.setSyncColorLabels(true);
        return s;
    }

    private ProfileEntity fullyConfiguredProfile() {
        var p = new ProfileEntity();
        p.setOutlookProfilePath("/path/to/outlook.pst");
        p.setOutlookCalendarId("cal-id");
        p.setGoogleCalendarId("gcal-id");
        return p;
    }

    /** Make logAroundRun actually execute the block so doSync logic runs. */
    @BeforeEach
    void setupLogAroundRun() {
        doAnswer(inv -> {
            Runnable block = inv.getArgument(2);
            block.run();
            return null;
        }).when(logService).logAroundRun(anyString(), anyBoolean(), any(Runnable.class));
        when(logService.saveRun(any())).thenAnswer(i -> i.getArgument(0));
    }

    /** Set the internal AtomicBoolean 'running' field via reflection. */
    private void forceRunningFlag(boolean value) throws Exception {
        Field field = SyncService.class.getDeclaredField("running");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(syncService)).set(value);
    }

    // ── isRunning ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isRunning")
    class IsRunning {

        @Test
        @DisplayName("returns false initially")
        void returnsFalseInitially() {
            assertThat(syncService.isRunning()).isFalse();
        }

        @Test
        @DisplayName("returns true when running flag is set")
        void returnsTrueWhenFlagSet() throws Exception {
            forceRunningFlag(true);
            assertThat(syncService.isRunning()).isTrue();
        }
    }

    // ── runSync - not already running ─────────────────────────────────────────

    @Nested
    @DisplayName("runSync - first call (not already running)")
    class RunSyncFirst {

        @Test
        @DisplayName("returns RUNNING entity (returned object before executeSync updates it)")
        void returnsEntityWithRunningIdAndTimestamp() {
            // Profile missing outlook path → doSync will fail; we only care about the return value
            var profile = new ProfileEntity(); // no outlookPath
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);

            var result = syncService.runSync();

            assertThat(result.getId()).isNotBlank();
            assertThat(result.getStartedAt()).isNotNull();
            // saveRun was called at least once
            verify(logService, atLeastOnce()).saveRun(any());
        }

        @Test
        @DisplayName("running flag is false after sync completes (executeSync clears it)")
        void runningFlagClearedAfterSync() {
            var profile = new ProfileEntity(); // will cause FAILED
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);

            syncService.runSync();

            assertThat(syncService.isRunning()).isFalse();
        }
    }

    // ── runSync - already running ─────────────────────────────────────────────

    @Nested
    @DisplayName("runSync - already running")
    class RunSyncAlreadyRunning {

        @Test
        @DisplayName("returns FAILED entity with 'already running' message")
        void returnsFailedEntityWhenAlreadyRunning() throws Exception {
            forceRunningFlag(true);

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(result.getMessage()).contains("already running");
            assertThat(result.getStartedAt()).isNotNull();
            assertThat(result.getFinishedAt()).isNotNull();
            verify(logService).saveRun(result);
        }

        @Test
        @DisplayName("does NOT clear running flag when skipping")
        void doesNotClearRunningFlag() throws Exception {
            forceRunningFlag(true);

            syncService.runSync();

            assertThat(syncService.isRunning()).isTrue();
        }
    }

    // ── doSync (tested indirectly via runSync) ────────────────────────────────

    @Nested
    @DisplayName("doSync - configuration validation")
    class DoSyncValidation {

        @Test
        @DisplayName("sets FAILED when outlookProfilePath is null")
        void failsWhenOutlookPathNull() {
            var profile = new ProfileEntity(); // null outlookProfilePath
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(result.getMessage()).contains("Outlook profile path");
        }

        @Test
        @DisplayName("sets FAILED when outlookCalendarId is null")
        void failsWhenOutlookCalendarIdNull() {
            var profile = new ProfileEntity();
            profile.setOutlookProfilePath("/path/to.pst");
            // no calendarId
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(result.getMessage()).contains("Outlook calendar");
        }

        @Test
        @DisplayName("sets FAILED when googleCalendarId is null")
        void failsWhenGoogleCalendarIdNull() {
            var profile = new ProfileEntity();
            profile.setOutlookProfilePath("/path/to.pst");
            profile.setOutlookCalendarId("cal");
            // no googleCalendarId
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(result.getMessage()).contains("Google calendar");
        }
    }

    @Nested
    @DisplayName("doSync - happy path")
    class DoSyncHappyPath {

        private ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        @Test
        @DisplayName("calls outlookService.readEvents and googleService.readEvents with calendar IDs")
        void callsExternalServicesWithCorrectIds() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any())).thenReturn(List.of());
            when(googleService.batchWrite(anyString(), any(), any(), any(), anyBoolean()))
                    .thenReturn(BatchWriteResult.empty());

            syncService.runSync();

            verify(outlookService).readEvents(eq("/path/to/outlook.pst"), eq("cal-id"), any());
            verify(googleService).readEvents(eq("gcal-id"), any());
        }

        @Test
        @DisplayName("marks run as SUCCESS with correct counts after batch write")
        void marksRunAsSuccessWithCounts() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);

            var outlookEvent = new OutlookEvent("id1", "Meeting", "body", null,
                    now, now.plusHours(1), false, 0);
            when(outlookService.readEvents(anyString(), anyString(), any()))
                    .thenReturn(List.of(outlookEvent));
            when(googleService.readEvents(anyString(), any())).thenReturn(List.of());
            when(googleService.batchWrite(anyString(), any(), any(), any(), anyBoolean()))
                    .thenReturn(new BatchWriteResult(1, 0, 0, 0));

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.SUCCESS);
            assertThat(result.getCreated()).isEqualTo(1);
            assertThat(result.getUpdated()).isEqualTo(0);
            assertThat(result.getDeleted()).isEqualTo(0);
        }

        @Test
        @DisplayName("routes unchanged Outlook events to create, changed to update, missing to delete")
        void routesEventsDiff() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);

            // Outlook: 2 events — one new, one existing-unchanged, one existing-changed
            var newEvent = new OutlookEvent("new-id", "New Meeting", null, null,
                    now, now.plusHours(1), false, 0);
            var changedEvent = new OutlookEvent("changed-id", "Updated", null, null,
                    now.plusDays(1), now.plusDays(1).plusHours(1), false, 0);
            var unchangedEvent = new OutlookEvent("same-id", "Same Meeting", null, null,
                    now.plusDays(2), now.plusDays(2).plusHours(1), false, 0);

            // Google: corresponds to changedEvent (different time) and unchangedEvent (same)
            var googleChanged = new GoogleEvent("gid-changed", "changed-id", "Old Title", null, null,
                    now.plusDays(3), now.plusDays(3).plusHours(1), false); // different time → update
            var googleUnchanged = new GoogleEvent("gid-same", "same-id", "Same Meeting", null, null,
                    now.plusDays(2), now.plusDays(2).plusHours(1), false); // same → skip
            var orphanGoogle = new GoogleEvent("gid-orphan", "orphan-id", "Orphan", null, null,
                    now, now.plusHours(1), false); // no Outlook counterpart → delete

            when(outlookService.readEvents(anyString(), anyString(), any()))
                    .thenReturn(List.of(newEvent, changedEvent, unchangedEvent));
            when(googleService.readEvents(anyString(), any()))
                    .thenReturn(List.of(googleChanged, googleUnchanged, orphanGoogle));
            when(googleService.batchWrite(anyString(), any(), any(), any(), anyBoolean()))
                    .thenReturn(new BatchWriteResult(1, 1, 1, 0));

            syncService.runSync();

            verify(googleService).batchWrite(
                    eq("gcal-id"),
                    argThat(creates -> creates.size() == 1 && creates.get(0).id().equals("new-id")),
                    argThat(updates -> updates.size() == 1 && updates.containsKey("gid-changed")),
                    argThat(deletes -> deletes.size() == 1 && deletes.contains("gid-orphan")),
                    eq(true)
            );
        }

        @Test
        @DisplayName("sets FAILED when outlookService throws exception")
        void failsWhenOutlookServiceThrows() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("PST parse error"));

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(result.getMessage()).contains("PST parse error");
        }

        @Test
        @DisplayName("always sets finishedAt even when sync throws")
        void setsFinishedAtOnFailure() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any()))
                    .thenThrow(new RuntimeException("fail"));

            var result = syncService.runSync();

            assertThat(result.getFinishedAt()).isNotNull();
        }
    }

    // ── doSync - Google token expiry ──────────────────────────────────────────

    @Nested
    @DisplayName("doSync - Google token expiry")
    class DoSyncGoogleTokenExpiry {

        private static final String INVALID_GRANT_MSG =
                "400 Bad Request POST https://oauth2.googleapis.com/token " +
                "{ \"error\": \"invalid_grant\", \"error_description\": \"Token has been expired or revoked.\" }";

        @Test
        @DisplayName("sets GOOGLE_TOKEN_EXPIRED when exception message contains 'invalid_grant'")
        void setsGoogleTokenExpiredOnInvalidGrant() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any()))
                    .thenThrow(new RuntimeException(INVALID_GRANT_MSG));

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.GOOGLE_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("sets user-friendly message on GOOGLE_TOKEN_EXPIRED — not the raw OAuth error")
        void setsFriendlyMessageOnTokenExpiry() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any()))
                    .thenThrow(new RuntimeException("invalid_grant"));

            var result = syncService.runSync();

            assertThat(result.getMessage())
                    .contains("Google token")
                    .contains("expired")
                    .doesNotContain("invalid_grant");
        }

        @Test
        @DisplayName("sets GOOGLE_TOKEN_EXPIRED when 'invalid_grant' is in a nested cause")
        void setsGoogleTokenExpiredOnNestedCause() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            var rootCause = new RuntimeException("invalid_grant");
            var wrapper   = new RuntimeException("Google API call failed", rootCause);
            when(googleService.readEvents(anyString(), any())).thenThrow(wrapper);

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.GOOGLE_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("sets GOOGLE_TOKEN_EXPIRED when wrapper has null message but cause has 'invalid_grant'")
        void setsGoogleTokenExpiredWhenWrapperMessageIsNull() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            var rootCause = new RuntimeException("invalid_grant");
            var wrapper   = new RuntimeException((String) null, rootCause);
            when(googleService.readEvents(anyString(), any())).thenThrow(wrapper);

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.GOOGLE_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("sets FAILED — not GOOGLE_TOKEN_EXPIRED — for a generic exception without 'invalid_grant'")
        void setsFailed_notTokenExpired_forGenericException() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any()))
                    .thenThrow(new RuntimeException("Connection timeout"));

            var result = syncService.runSync();

            assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
        }

        @Test
        @DisplayName("preserves raw error message in FAILED run for non-token exceptions")
        void preservesRawMessageInFailedRun() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any()))
                    .thenThrow(new RuntimeException("Connection timeout"));

            var result = syncService.runSync();

            assertThat(result.getMessage()).contains("Connection timeout");
        }

        @Test
        @DisplayName("sets finishedAt on GOOGLE_TOKEN_EXPIRED")
        void setsFinishedAtOnTokenExpiry() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any()))
                    .thenThrow(new RuntimeException(INVALID_GRANT_MSG));

            var result = syncService.runSync();

            assertThat(result.getFinishedAt()).isNotNull();
        }

        @Test
        @DisplayName("logs error mentioning 'Google token expired' on GOOGLE_TOKEN_EXPIRED")
        void logsErrorWithTokenExpiredMessage() {
            var profile = fullyConfiguredProfile();
            when(settingsService.getOrCreate()).thenReturn(defaultSettings());
            when(profileService.getOrCreate()).thenReturn(profile);
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any()))
                    .thenThrow(new RuntimeException(INVALID_GRANT_MSG));

            syncService.runSync();

            verify(logService, atLeastOnce()).error(contains("Google token expired"));
        }
    }
}
