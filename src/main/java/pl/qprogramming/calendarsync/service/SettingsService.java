package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.repository.SyncSettingsRepository;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SyncSettingsRepository settingsRepository;

    public SyncSettingsEntity getOrCreate() {
        return settingsRepository.findById(1L).orElseGet(() -> settingsRepository.save(new SyncSettingsEntity()));
    }

    public SyncSettingsEntity update(int frequencyMinutes, int daysPast, int daysFuture, boolean debugLogging) {
        SyncSettingsEntity settings = getOrCreate();
        settings.setFrequencyMinutes(frequencyMinutes);
        settings.setDaysPast(daysPast);
        settings.setDaysFuture(daysFuture);
        settings.setDebugLogging(debugLogging);
        return settingsRepository.save(settings);
    }
}
