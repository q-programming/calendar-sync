package pl.qprogramming.calendarsync.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.repository.SyncSettingsRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsService")
class SettingsServiceTest {

    private static final String APP_PACKAGE = "pl.qprogramming.calendarsync";

    @Mock
    SyncSettingsRepository settingsRepository;

    @InjectMocks
    SettingsService settingsService;

    @AfterEach
    void resetLogLevel() {
        // Restore INFO level after each test to avoid state leakage
        LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
        ctx.getLogger(APP_PACKAGE).setLevel(Level.INFO);
    }

    // ── getOrCreate ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("returns existing entity when present in repository")
        void returnsExisting_WhenPresent() {
            var existing = new SyncSettingsEntity();
            existing.setFrequencyMinutes(30);
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(existing));

            var result = settingsService.getOrCreate();

            assertThat(result).isEqualTo(existing);
            assertThat(result.getFrequencyMinutes()).isEqualTo(30);
            verify(settingsRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates and saves new entity when not found")
        void createsNew_WhenNotFound() {
            var saved = new SyncSettingsEntity();
            when(settingsRepository.findById(1L)).thenReturn(Optional.empty());
            when(settingsRepository.save(any())).thenReturn(saved);

            var result = settingsService.getOrCreate();

            assertThat(result).isEqualTo(saved);
            verify(settingsRepository).save(any(SyncSettingsEntity.class));
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("saves all fields and returns saved entity")
        void savesAllFields() {
            var existing = new SyncSettingsEntity();
            var saved = new SyncSettingsEntity();
            saved.setFrequencyMinutes(15);
            saved.setDaysPast(14);
            saved.setDaysFuture(60);
            saved.setDebugLogging(false);
            saved.setSyncColorLabels(false);

            when(settingsRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(settingsRepository.save(any())).thenReturn(saved);

            var result = settingsService.update(15, 14, 60, false, false);

            assertThat(result).isEqualTo(saved);
            verify(settingsRepository).save(argThat(e ->
                    e.getFrequencyMinutes() == 15
                    && e.getDaysPast() == 14
                    && e.getDaysFuture() == 60
                    && !e.isDebugLogging()
                    && !e.isSyncColorLabels()
            ));
        }

        @Test
        @DisplayName("sets Logback level to DEBUG when debugLogging=true")
        void setsDebugLevel_WhenDebugLoggingTrue() {
            var existing = new SyncSettingsEntity();
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(settingsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            settingsService.update(60, 7, 30, true, true);

            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            assertThat(ctx.getLogger(APP_PACKAGE).getLevel()).isEqualTo(Level.DEBUG);
        }

        @Test
        @DisplayName("sets Logback level to INFO when debugLogging=false")
        void setsInfoLevel_WhenDebugLoggingFalse() {
            // First set to DEBUG
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            ctx.getLogger(APP_PACKAGE).setLevel(Level.DEBUG);

            var existing = new SyncSettingsEntity();
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(settingsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            settingsService.update(60, 7, 30, false, true);

            assertThat(ctx.getLogger(APP_PACKAGE).getLevel()).isEqualTo(Level.INFO);
        }
    }

    // ── applyStoredLogLevel ───────────────────────────────────────────────────

    @Nested
    @DisplayName("applyStoredLogLevel")
    class ApplyStoredLogLevel {

        @Test
        @DisplayName("applies DEBUG when stored settings have debugLogging=true")
        void appliesDebug_WhenStored() {
            var settings = new SyncSettingsEntity();
            settings.setDebugLogging(true);
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));

            settingsService.applyStoredLogLevel();

            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            assertThat(ctx.getLogger(APP_PACKAGE).getLevel()).isEqualTo(Level.DEBUG);
        }

        @Test
        @DisplayName("applies INFO when stored settings have debugLogging=false")
        void appliesInfo_WhenStored() {
            var settings = new SyncSettingsEntity();
            settings.setDebugLogging(false);
            when(settingsRepository.findById(1L)).thenReturn(Optional.of(settings));

            // Pre-set DEBUG to verify it gets reset
            LoggerContext ctx = (LoggerContext) LoggerFactory.getILoggerFactory();
            ctx.getLogger(APP_PACKAGE).setLevel(Level.DEBUG);

            settingsService.applyStoredLogLevel();

            assertThat(ctx.getLogger(APP_PACKAGE).getLevel()).isEqualTo(Level.INFO);
        }
    }
}
