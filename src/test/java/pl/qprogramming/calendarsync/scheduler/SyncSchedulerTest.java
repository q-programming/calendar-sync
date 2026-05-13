package pl.qprogramming.calendarsync.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.repository.ProfileRepository;
import pl.qprogramming.calendarsync.repository.SyncSettingsRepository;
import pl.qprogramming.calendarsync.service.LogService;
import pl.qprogramming.calendarsync.service.SyncService;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncScheduler")
class SyncSchedulerTest {

    @Mock TaskScheduler taskScheduler;
    @Mock SyncSettingsRepository settingsRepository;
    @Mock SyncService syncService;
    @Mock ProfileRepository profileRepository;
    @Mock LogService logService;

    @InjectMocks
    SyncScheduler scheduler;

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ScheduledFuture<?> newMockFuture() {
        return mock(ScheduledFuture.class);
    }

    @SuppressWarnings({"unchecked"})
    private void stubSchedule() {
        doReturn(newMockFuture()).when(taskScheduler)
                .scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
    }

    /** Set up schedule stub, call reschedule(), capture and return the registered Runnable. */
    private Runnable captureScheduledTask() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(inv -> newMockFuture()).when(taskScheduler)
                .scheduleWithFixedDelay(runnableCaptor.capture(), any(Duration.class));
        scheduler.reschedule();
        return runnableCaptor.getValue();
    }

    // ── init ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("calls removeOldEntries, failStaleSyncRuns, then reschedule")
        void callsHousekeepingThenReschedule() {
            when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
            stubSchedule();

            scheduler.init();

            verify(logService).removeOldEntries();
            verify(logService).failStaleSyncRuns();
            verify(taskScheduler).scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));
        }
    }

    // ── reschedule ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reschedule")
    class Reschedule {

        @Test
        @DisplayName("schedules with default 60 minutes when settings not found")
        void schedulesDefault60Minutes() {
            when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
            doAnswer(inv -> newMockFuture()).when(taskScheduler)
                    .scheduleWithFixedDelay(any(Runnable.class), durationCaptor.capture());

            scheduler.reschedule();

            assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMinutes(60));
        }

        @Test
        @DisplayName("schedules with stored frequency when settings found")
        void schedulesStoredFrequency() {
            var settings = new SyncSettingsEntity();
            settings.setFrequencyMinutes(15);
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));
            ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
            doAnswer(inv -> newMockFuture()).when(taskScheduler)
                    .scheduleWithFixedDelay(any(Runnable.class), durationCaptor.capture());

            scheduler.reschedule();

            assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMinutes(15));
        }

        @Test
        @DisplayName("cancels previous task on second reschedule")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void cancelsPreviousTaskOnReschedule() {
            when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
            ScheduledFuture<?> firstFuture = newMockFuture();
            when(firstFuture.isCancelled()).thenReturn(false);

            doReturn(firstFuture, newMockFuture()).when(taskScheduler)
                    .scheduleWithFixedDelay(any(Runnable.class), any(Duration.class));

            scheduler.reschedule();
            scheduler.reschedule();

            verify(firstFuture).cancel(false);
        }
    }

    // ── runScheduledSync (tested via captured Runnable) ───────────────────────

    @Nested
    @DisplayName("runScheduledSync")
    class RunScheduledSync {

        @BeforeEach
        void setupSettings() {
            when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("skips sync when no profile exists")
        void skipsSync_WhenNoProfile() {
            when(profileRepository.findById(1L)).thenReturn(Optional.empty());

            Runnable task = captureScheduledTask();
            task.run();

            verifyNoInteractions(syncService);
        }

        @Test
        @DisplayName("skips sync when outlookProfilePath is null")
        void skipsSync_WhenNoOutlookPath() {
            var profile = new ProfileEntity();
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

            Runnable task = captureScheduledTask();
            task.run();

            verifyNoInteractions(syncService);
        }

        @Test
        @DisplayName("skips sync when googleCalendarId is null")
        void skipsSync_WhenNoGoogleCalendarId() {
            var profile = new ProfileEntity();
            profile.setOutlookProfilePath("/path/to.pst");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

            Runnable task = captureScheduledTask();
            task.run();

            verifyNoInteractions(syncService);
        }

        @Test
        @DisplayName("skips sync when googlePrincipalName is blank")
        void skipsSync_WhenNoGooglePrincipal() {
            var profile = new ProfileEntity();
            profile.setOutlookProfilePath("/path/to.pst");
            profile.setGoogleCalendarId("gcal");
            profile.setGooglePrincipalName("   ");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

            Runnable task = captureScheduledTask();
            task.run();

            verifyNoInteractions(syncService);
        }

        @Test
        @DisplayName("calls syncService.runSync when profile fully configured")
        void callsSyncService_WhenProfileConfigured() {
            var profile = new ProfileEntity();
            profile.setOutlookProfilePath("/path/to.pst");
            profile.setGoogleCalendarId("gcal");
            profile.setGooglePrincipalName("user@example.com");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

            Runnable task = captureScheduledTask();
            task.run();

            verify(syncService).runSync();
        }

        @Test
        @DisplayName("catches exception from syncService.runSync without propagating")
        void catchesExceptionFromSyncService() {
            var profile = new ProfileEntity();
            profile.setOutlookProfilePath("/path/to.pst");
            profile.setGoogleCalendarId("gcal");
            profile.setGooglePrincipalName("user@example.com");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(syncService.runSync()).thenThrow(new RuntimeException("sync failed"));

            Runnable task = captureScheduledTask();
            task.run(); // Should not throw
        }
    }
}
