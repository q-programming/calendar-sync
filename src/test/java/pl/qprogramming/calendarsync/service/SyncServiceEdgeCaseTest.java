package pl.qprogramming.calendarsync.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncRunStatus;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.service.google.BatchWriteResult;
import pl.qprogramming.calendarsync.service.google.GoogleCalendarService;
import pl.qprogramming.calendarsync.service.google.GoogleEvent;
import pl.qprogramming.calendarsync.service.outlook.OutlookCalendarService;
import pl.qprogramming.calendarsync.service.outlook.OutlookEvent;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Additional SyncService tests to cover edge cases not in SyncServiceOrchestrationTest.
 * Focuses on: formatEventTime/formatDate null paths, warn on batch failures, color label flags.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SyncService (edge cases)")
class SyncServiceEdgeCaseTest {

    @Mock OutlookCalendarService outlookService;
    @Mock GoogleCalendarService googleService;
    @Mock ProfileService profileService;
    @Mock SettingsService settingsService;
    @Mock LogService logService;

    @InjectMocks SyncService syncService;

    private SyncSettingsEntity settings(boolean debug, boolean colors) {
        var s = new SyncSettingsEntity();
        s.setDaysPast(7);
        s.setDaysFuture(30);
        s.setDebugLogging(debug);
        s.setSyncColorLabels(colors);
        return s;
    }

    private ProfileEntity fullProfile() {
        var p = new ProfileEntity();
        p.setOutlookProfilePath("/path.pst");
        p.setOutlookCalendarId("oc");
        p.setGoogleCalendarId("gc");
        return p;
    }

    @org.junit.jupiter.api.BeforeEach
    void setupLogAroundRun() {
        doAnswer(inv -> { ((Runnable) inv.getArgument(2)).run(); return null; })
                .when(logService).logAroundRun(anyString(), anyBoolean(), any(Runnable.class));
        when(logService.saveRun(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ── batchWrite warns on failures ──────────────────────────────────────────

    @Nested
    @DisplayName("doSync warns when batch write has failures")
    class BatchFailures {

        @Test
        @DisplayName("calls logService.warn when batchWrite.failed > 0")
        void warnsOnBatchFailure() {
            when(settingsService.getOrCreate()).thenReturn(settings(false, true));
            when(profileService.getOrCreate()).thenReturn(fullProfile());
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any())).thenReturn(List.of());
            when(googleService.batchWrite(anyString(), any(), any(), any(), anyBoolean()))
                    .thenReturn(new BatchWriteResult(0, 0, 0, 2)); // 2 failures

            syncService.runSync();

            verify(logService, atLeastOnce()).warn(contains("failed"), anyInt());
        }

        @Test
        @DisplayName("does NOT warn when batch write has no failures")
        void doesNotWarnOnZeroFailures() {
            when(settingsService.getOrCreate()).thenReturn(settings(false, true));
            when(profileService.getOrCreate()).thenReturn(fullProfile());
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any())).thenReturn(List.of());
            when(googleService.batchWrite(anyString(), any(), any(), any(), anyBoolean()))
                    .thenReturn(BatchWriteResult.empty());

            syncService.runSync();

            verify(logService, never()).warn(contains("failed"), anyInt());
        }
    }

    // ── syncColorLabels flag is forwarded ─────────────────────────────────────

    @Nested
    @DisplayName("syncColorLabels flag forwarding")
    class ColorLabels {

        @ParameterizedTest
        @CsvSource({"true", "false"})
        @DisplayName("passes syncColorLabels setting to batchWrite")
        void passesSyncColorLabelsFlag(boolean colorLabels) {
            when(settingsService.getOrCreate()).thenReturn(settings(false, colorLabels));
            when(profileService.getOrCreate()).thenReturn(fullProfile());
            when(outlookService.readEvents(anyString(), anyString(), any())).thenReturn(List.of());
            when(googleService.readEvents(anyString(), any())).thenReturn(List.of());
            when(googleService.batchWrite(anyString(), any(), any(), any(), eq(colorLabels)))
                    .thenReturn(BatchWriteResult.empty());

            syncService.runSync();

            verify(googleService).batchWrite(anyString(), any(), any(), any(), eq(colorLabels));
        }
    }

    // ── eqInstant edge cases ──────────────────────────────────────────────────

    @Nested
    @DisplayName("eqInstant edge cases")
    class EqInstant {

        private final ZonedDateTime t = ZonedDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);

        @Test
        @DisplayName("null+null → true")
        void bothNull() {
            assertThat(syncService.eqInstant(null, null)).isTrue();
        }

        @Test
        @DisplayName("null+non-null → false")
        void firstNull() {
            assertThat(syncService.eqInstant(null, t)).isFalse();
        }

        @Test
        @DisplayName("non-null+null → false")
        void secondNull() {
            assertThat(syncService.eqInstant(t, null)).isFalse();
        }

        @Test
        @DisplayName("same instant different zone → true")
        void sameInstantDifferentZone() {
            var berlin = t.withZoneSameInstant(java.time.ZoneId.of("Europe/Berlin"));
            assertThat(syncService.eqInstant(t, berlin)).isTrue();
        }
    }

    // ── blank outlookCalendarId check ─────────────────────────────────────────

    @Test
    @DisplayName("FAILED when outlookCalendarId is blank string")
    void failsWhenOutlookCalendarIdBlank() {
        var profile = new ProfileEntity();
        profile.setOutlookProfilePath("/path.pst");
        profile.setOutlookCalendarId("   "); // blank but not null
        when(settingsService.getOrCreate()).thenReturn(settings(false, true));
        when(profileService.getOrCreate()).thenReturn(profile);

        var result = syncService.runSync();

        assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
    }

    @Test
    @DisplayName("FAILED when googleCalendarId is blank string")
    void failsWhenGoogleCalendarIdBlank() {
        var profile = new ProfileEntity();
        profile.setOutlookProfilePath("/path.pst");
        profile.setOutlookCalendarId("oc");
        profile.setGoogleCalendarId(""); // empty
        when(settingsService.getOrCreate()).thenReturn(settings(false, true));
        when(profileService.getOrCreate()).thenReturn(profile);

        var result = syncService.runSync();

        assertThat(result.getStatus()).isEqualTo(SyncRunStatus.FAILED);
    }
}
