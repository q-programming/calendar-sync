package pl.qprogramming.calendarsync.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.repository.ProfileRepository;
import pl.qprogramming.calendarsync.repository.SyncSettingsRepository;
import pl.qprogramming.calendarsync.service.LogService;
import pl.qprogramming.calendarsync.service.SyncService;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

/**
 * Drives the periodic Outlook → Google Calendar sync on a configurable fixed-delay schedule.
 *
 * <p>On startup ({@link #init}) the scheduler performs two housekeeping tasks before
 * starting the first schedule:
 * <ol>
 *   <li><b>Remove old entries</b> – deletes sync run records and their log entries that are
 *       older than 30 days to prevent unbounded database growth.</li>
 *   <li><b>Fail stale runs</b> – any run left in {@code RUNNING} state from a previous JVM
 *       process is marked as {@code FAILED} with an explanatory message, so the UI does not
 *       show phantom in-progress syncs after a restart.</li>
 * </ol>
 *
 * <p>The schedule is dynamic: calling {@link #reschedule()} cancels the currently active
 * task and registers a new one with the latest frequency from the settings table.
 * This allows users to change the sync interval at runtime without restarting the application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final TaskScheduler taskScheduler;
    private final SyncSettingsRepository settingsRepository;
    private final SyncService syncService;
    private final ProfileRepository profileRepository;
    private final LogService logService;

    private ScheduledFuture<?> currentTask;

    /**
     * Runs housekeeping on startup, then arms the periodic sync schedule.
     *
     * <ul>
     *   <li>Removes log entries and run records older than 30 days.</li>
     *   <li>Transitions any orphaned {@code RUNNING} runs to {@code FAILED}.</li>
     *   <li>Schedules the first (and subsequent) periodic sync via {@link #reschedule()}.</li>
     * </ul>
     */
    @PostConstruct
    public void init() {
        logService.removeOldEntries();
        logService.failStaleSyncRuns();
        reschedule();
    }

    /**
     * Cancels the currently scheduled sync task (if any) and registers a new one using
     * the frequency stored in the settings table (defaulting to 60 minutes if not set).
     *
     * <p>Synchronized to prevent concurrent reschedule calls from overlapping, e.g. when
     * settings are saved while a reschedule triggered by the previous save is still running.
     */
    public synchronized void reschedule() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
        }
        int minutes = settingsRepository.findById(1L)
                .map(SyncSettingsEntity::getFrequencyMinutes)
                .orElse(60);
        Duration interval = Duration.ofMinutes(minutes);
        log.info("Scheduling periodic sync every {} minutes", minutes);
        currentTask = taskScheduler.scheduleWithFixedDelay(this::runScheduledSync, interval);
    }

    /**
     * Invoked by the scheduler on every tick. Triggers a sync only when the profile is
     * fully configured (Outlook path, Google calendar, and Google principal all present).
     * Silently skips the tick with a DEBUG log when any prerequisite is missing.
     */
    private void runScheduledSync() {
        log.info("Running scheduled sync");
        profileRepository.findById(1L).ifPresent(profile -> {
            try {
                if (profile.getOutlookProfilePath() == null || profile.getGoogleCalendarId() == null) {
                    log.debug("Skipping scheduled sync: profile not fully configured");
                    return;
                }
                if (profile.getGooglePrincipalName() == null || profile.getGooglePrincipalName().isBlank()) {
                    log.debug("Skipping scheduled sync: no Google principal stored yet");
                    return;
                }
                syncService.runSync();
            } catch (Exception e) {
                log.error("Scheduled sync failed", e);
            }
        });
    }
}
