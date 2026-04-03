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

/**
 * Manages application-wide synchronisation settings stored in the database.
 *
 * <p>Settings are kept in a single row ({@code id = 1}) of the {@code sync_settings} table.
 * A row is auto-created on first access so callers never receive {@code null}.
 *
 * <p>In addition to CRUD, this service owns the Logback log-level lifecycle:
 * the configured debug flag is re-applied every time settings are saved and
 * once more on application startup so the level survives restarts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String APP_PACKAGE = "pl.qprogramming.calendarsync";

    private final SyncSettingsRepository settingsRepository;

    /**
     * Returns the current settings row, creating a default one if none exists yet.
     *
     * @return the singleton {@link SyncSettingsEntity}; never {@code null}
     */
    public SyncSettingsEntity getOrCreate() {
        return settingsRepository.findById(1L).orElseGet(() -> settingsRepository.save(new SyncSettingsEntity()));
    }

    /**
     * Persists updated sync settings and immediately applies the new log level.
     *
     * @param frequencyMinutes how often the scheduler should trigger a sync (minutes)
     * @param daysPast         how many days into the past the sync window extends
     * @param daysFuture       how many days into the future the sync window extends
     * @param debugLogging     {@code true} to enable DEBUG-level logging for the app package
     * @param syncColorLabels  {@code true} to mirror Outlook category colours to Google event colours
     * @return the saved {@link SyncSettingsEntity}
     */
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

    /**
     * Adjusts the Logback log level for the application's root package at runtime.
     *
     * <p>When {@code debug} is {@code true} the level is set to {@link Level#DEBUG};
     * otherwise it falls back to {@link Level#INFO}.
     *
     * @param debug {@code true} to switch to DEBUG level, {@code false} for INFO
     */
    private void applyLogLevel(boolean debug) {
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        Level level = debug ? Level.DEBUG : Level.INFO;
        ctx.getLogger(APP_PACKAGE).setLevel(level);
        log.info("Log level for '{}' set to {}", APP_PACKAGE, level);
    }
}
