package pl.qprogramming.calendarsync.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.repository.SyncSettingsRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String APP_PACKAGE = "pl.qprogramming.calendarsync";

    private final SyncSettingsRepository settingsRepository;

    public SyncSettingsEntity getOrCreate() {
        return settingsRepository.findById(1L).orElseGet(() -> settingsRepository.save(new SyncSettingsEntity()));
    }

    public SyncSettingsEntity update(int frequencyMinutes, int daysPast, int daysFuture, boolean debugLogging, boolean syncColorLabels) {
        SyncSettingsEntity settings = getOrCreate();
        settings.setFrequencyMinutes(frequencyMinutes);
        settings.setDaysPast(daysPast);
        settings.setDaysFuture(daysFuture);
        settings.setDebugLogging(debugLogging);
        settings.setSyncColorLabels(syncColorLabels);
        SyncSettingsEntity saved = settingsRepository.save(settings);
        applyLogLevel(debugLogging);
        return saved;
    }

    /** Apply stored debug setting on startup so log level survives restarts. */
    @EventListener(ApplicationReadyEvent.class)
    public void applyStoredLogLevel() {
        applyLogLevel(getOrCreate().isDebugLogging());
    }

    private void applyLogLevel(boolean debug) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Level level = debug ? Level.DEBUG : Level.INFO;
        ctx.getLogger(APP_PACKAGE).setLevel(level);
        log.info("Log level for '{}' set to {}", APP_PACKAGE, level);
    }
}
