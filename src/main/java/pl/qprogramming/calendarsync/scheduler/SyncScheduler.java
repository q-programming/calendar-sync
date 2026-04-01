package pl.qprogramming.calendarsync.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.repository.ProfileRepository;
import pl.qprogramming.calendarsync.repository.SyncSettingsRepository;
import pl.qprogramming.calendarsync.service.SyncService;

import java.security.Principal;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncScheduler {

    private final TaskScheduler taskScheduler;
    private final SyncSettingsRepository settingsRepository;
    private final SyncService syncService;
    private final ProfileRepository profileRepository;

    private ScheduledFuture<?> currentTask;

    @PostConstruct
    public void init() {
        reschedule();
    }

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

    private void runScheduledSync() {
        log.info("Running scheduled sync");
        profileRepository.findById(1L).ifPresent(profile -> {
            try {
                if (profile.getOutlookProfilePath() == null || profile.getGoogleCalendarId() == null) {
                    log.debug("Skipping scheduled sync: profile not fully configured");
                    return;
                }
                String principalName = profile.getGooglePrincipalName();
                if (principalName == null || principalName.isBlank()) {
                    log.debug("Skipping scheduled sync: no Google principal stored yet");
                    return;
                }
                Principal principal = () -> principalName;
                syncService.runSync(principal);
            } catch (Exception e) {
                log.error("Scheduled sync failed", e);
            }
        });
    }
}
