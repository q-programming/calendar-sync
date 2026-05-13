package pl.qprogramming.calendarsync.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pl.qprogramming.calendarsync.dto.SyncRunStatus;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.service.SyncService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SyncController")
class SyncControllerTest {

    @Mock SyncService syncService;

    @InjectMocks
    SyncController controller;

    private SyncRunEntity buildRun(pl.qprogramming.calendarsync.entity.SyncRunStatus status) {
        var run = new SyncRunEntity();
        run.setId("run-abc");
        run.setStartedAt(OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        run.setFinishedAt(OffsetDateTime.of(2025, 6, 1, 10, 1, 0, 0, ZoneOffset.UTC));
        run.setStatus(status);
        run.setCreated(3);
        run.setUpdated(2);
        run.setDeleted(1);
        run.setMessage("Sync: +3 ~2 -1");
        return run;
    }

    // ── triggerSync ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("triggerSync")
    class TriggerSync {

        @Test
        @DisplayName("returns 200 with mapped DTO for a SUCCESS run")
        void returns200WithSuccessDto() {
            when(syncService.runSync()).thenReturn(buildRun(pl.qprogramming.calendarsync.entity.SyncRunStatus.SUCCESS));

            var response = controller.triggerSync();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var dto = response.getBody();
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo("run-abc");
            assertThat(dto.getStatus()).isEqualTo(SyncRunStatus.SUCCESS);
            assertThat(dto.getCreated()).isEqualTo(3);
            assertThat(dto.getUpdated()).isEqualTo(2);
            assertThat(dto.getDeleted()).isEqualTo(1);
            assertThat(dto.getMessage()).isEqualTo("Sync: +3 ~2 -1");
            assertThat(dto.getStartedAt()).isEqualTo("2025-06-01T10:00:00Z");
            assertThat(dto.getFinishedAt()).isEqualTo("2025-06-01T10:01:00Z");
        }

        @Test
        @DisplayName("maps entity FAILED status to DTO FAILED status")
        void mapsFailedStatus() {
            when(syncService.runSync()).thenReturn(buildRun(pl.qprogramming.calendarsync.entity.SyncRunStatus.FAILED));

            var response = controller.triggerSync();

            assertThat(response.getBody().getStatus()).isEqualTo(SyncRunStatus.FAILED);
        }

        @Test
        @DisplayName("maps entity RUNNING status to DTO RUNNING status")
        void mapsRunningStatus() {
            var run = buildRun(pl.qprogramming.calendarsync.entity.SyncRunStatus.RUNNING);
            run.setFinishedAt(null); // RUNNING has no finishedAt
            when(syncService.runSync()).thenReturn(run);

            var response = controller.triggerSync();

            assertThat(response.getBody().getStatus()).isEqualTo(SyncRunStatus.RUNNING);
            assertThat(response.getBody().getFinishedAt()).isNull();
        }

        @Test
        @DisplayName("handles null entity status gracefully")
        void handlesNullStatus() {
            var run = new SyncRunEntity();
            run.setId("no-status");
            run.setCreated(0);
            run.setUpdated(0);
            run.setDeleted(0);
            // status = null
            when(syncService.runSync()).thenReturn(run);

            var response = controller.triggerSync();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getStatus()).isNull();
        }
    }
}
