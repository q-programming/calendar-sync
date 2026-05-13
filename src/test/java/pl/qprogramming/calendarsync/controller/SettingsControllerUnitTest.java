package pl.qprogramming.calendarsync.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pl.qprogramming.calendarsync.dto.SyncSettings;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;
import pl.qprogramming.calendarsync.scheduler.SyncScheduler;
import pl.qprogramming.calendarsync.service.SettingsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsController (unit)")
class SettingsControllerUnitTest {

    @Mock SettingsService settingsService;
    @Mock SyncScheduler syncScheduler;

    @InjectMocks SettingsController controller;

    private SyncSettingsEntity buildEntity(int freq, int past, int future, boolean debug, boolean colors) {
        var e = new SyncSettingsEntity();
        e.setFrequencyMinutes(freq);
        e.setDaysPast(past);
        e.setDaysFuture(future);
        e.setDebugLogging(debug);
        e.setSyncColorLabels(colors);
        return e;
    }

    // ── getSettings ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSettings")
    class GetSettings {

        @Test
        @DisplayName("returns 200 with mapped DTO from entity")
        void returns200WithMappedDto() {
            when(settingsService.getOrCreate()).thenReturn(buildEntity(30, 14, 60, true, false));

            var response = controller.getSettings();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var dto = response.getBody();
            assertThat(dto).isNotNull();
            assertThat(dto.getFrequencyMinutes()).isEqualTo(30);
            assertThat(dto.getDaysPast()).isEqualTo(14);
            assertThat(dto.getDaysFuture()).isEqualTo(60);
            assertThat(dto.getDebugLogging()).isTrue();
            assertThat(dto.getSyncColorLabels()).isFalse();
        }

        @Test
        @DisplayName("maps default entity values correctly")
        void mapsDefaultValues() {
            when(settingsService.getOrCreate()).thenReturn(new SyncSettingsEntity());

            var response = controller.getSettings();

            var dto = response.getBody();
            assertThat(dto.getFrequencyMinutes()).isEqualTo(60);
            assertThat(dto.getDaysPast()).isEqualTo(7);
            assertThat(dto.getDaysFuture()).isEqualTo(30);
            assertThat(dto.getDebugLogging()).isFalse();
            assertThat(dto.getSyncColorLabels()).isTrue();
        }
    }

    // ── updateSettings ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateSettings")
    class UpdateSettings {

        @Test
        @DisplayName("returns 204 and calls service with all fields")
        void returns204AndCallsService() {
            var req = new SyncSettings()
                    .frequencyMinutes(15)
                    .daysPast(7)
                    .daysFuture(30)
                    .debugLogging(true)
                    .syncColorLabels(false);

            when(settingsService.update(anyInt(), anyInt(), anyInt(), anyBoolean(), anyBoolean()))
                    .thenReturn(new SyncSettingsEntity());

            var response = controller.updateSettings(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(settingsService).update(15, 7, 30, true, false);
        }

        @Test
        @DisplayName("calls syncScheduler.reschedule after update")
        void callsRescheduleAfterUpdate() {
            var req = new SyncSettings()
                    .frequencyMinutes(60)
                    .daysPast(7)
                    .daysFuture(30)
                    .debugLogging(false)
                    .syncColorLabels(true);

            when(settingsService.update(anyInt(), anyInt(), anyInt(), anyBoolean(), anyBoolean()))
                    .thenReturn(new SyncSettingsEntity());

            controller.updateSettings(req);

            verify(syncScheduler).reschedule();
        }

        @Test
        @DisplayName("handles null debugLogging and syncColorLabels as false")
        void handlesNullBooleans() {
            var req = new SyncSettings()
                    .frequencyMinutes(60)
                    .daysPast(7)
                    .daysFuture(30);
            // debugLogging and syncColorLabels are null

            when(settingsService.update(anyInt(), anyInt(), anyInt(), anyBoolean(), anyBoolean()))
                    .thenReturn(new SyncSettingsEntity());

            var response = controller.updateSettings(req);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(settingsService).update(60, 7, 30, false, false);
        }
    }
}
