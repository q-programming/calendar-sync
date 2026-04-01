package pl.qprogramming.calendarsync.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.api.SettingsApiDelegate;
import pl.qprogramming.calendarsync.dto.SyncSettings;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.scheduler.SyncScheduler;
import pl.qprogramming.calendarsync.service.SettingsService;

@Component
@RequiredArgsConstructor
public class SettingsController implements SettingsApiDelegate {

    private final SettingsService settingsService;
    private final SyncScheduler syncScheduler;

    @Override
    public ResponseEntity<SyncSettings> getSettings() {
        return ResponseEntity.ok(toDto(settingsService.getOrCreate()));
    }

    @Override
    public ResponseEntity<Void> updateSettings(SyncSettings syncSettings) {
        settingsService.update(
                syncSettings.getFrequencyMinutes(),
                syncSettings.getDaysPast(),
                syncSettings.getDaysFuture(),
                Boolean.TRUE.equals(syncSettings.getDebugLogging()));
        syncScheduler.reschedule();
        return ResponseEntity.noContent().build();
    }

    private SyncSettings toDto(SyncSettingsEntity e) {
        return new SyncSettings()
                .frequencyMinutes(e.getFrequencyMinutes())
                .daysPast(e.getDaysPast())
                .daysFuture(e.getDaysFuture())
                .debugLogging(e.isDebugLogging());
    }
}
