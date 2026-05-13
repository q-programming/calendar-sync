package pl.qprogramming.calendarsync.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import pl.qprogramming.calendarsync.dto.LogLevel;
import pl.qprogramming.calendarsync.dto.SyncRunStatus;
import pl.qprogramming.calendarsync.entity.SyncLogEntryEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.service.LogService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LogsController (unit)")
class LogsControllerUnitTest {

    @Mock LogService logService;

    @InjectMocks LogsController controller;

    private SyncRunEntity makeRun(String id, pl.qprogramming.calendarsync.entity.SyncRunStatus status) {
        var run = new SyncRunEntity();
        run.setId(id);
        run.setStartedAt(OffsetDateTime.of(2025, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        run.setFinishedAt(OffsetDateTime.of(2025, 1, 1, 10, 5, 0, 0, ZoneOffset.UTC));
        run.setStatus(status);
        run.setCreated(1);
        run.setUpdated(2);
        run.setDeleted(3);
        run.setMessage("test");
        return run;
    }

    // ── getLogs ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLogs")
    class GetLogs {

        @Test
        @DisplayName("uses default page=0 size=20 when params are null")
        void usesDefaults_WhenParamsNull() {
            var page = new PageImpl<SyncRunEntity>(List.of(), PageRequest.of(0, 20), 0);
            when(logService.getPagedRuns(0, 20, null)).thenReturn(page);

            var response = controller.getLogs(null, null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(logService).getPagedRuns(0, 20, null);
        }

        @Test
        @DisplayName("passes explicit page and size to service")
        void passesExplicitPageAndSize() {
            var page = new PageImpl<SyncRunEntity>(List.of(), PageRequest.of(2, 5), 0);
            when(logService.getPagedRuns(2, 5, null)).thenReturn(page);

            controller.getLogs(2, 5, null);

            verify(logService).getPagedRuns(2, 5, null);
        }

        @Test
        @DisplayName("maps status DTO to entity enum and passes to service")
        void mapsStatusToEntityEnum() {
            var page = new PageImpl<SyncRunEntity>(List.of(), PageRequest.of(0, 20), 0);
            when(logService.getPagedRuns(0, 20,
                    pl.qprogramming.calendarsync.entity.SyncRunStatus.SUCCESS)).thenReturn(page);

            controller.getLogs(null, null, SyncRunStatus.SUCCESS);

            verify(logService).getPagedRuns(0, 20, pl.qprogramming.calendarsync.entity.SyncRunStatus.SUCCESS);
        }

        @Test
        @DisplayName("maps page metadata correctly in response")
        void mapsPageMetadata() {
            var run = makeRun("r1", pl.qprogramming.calendarsync.entity.SyncRunStatus.SUCCESS);
            var page = new PageImpl<>(List.of(run), PageRequest.of(1, 5), 12);
            when(logService.getPagedRuns(1, 5, null)).thenReturn(page);

            var response = controller.getLogs(1, 5, null);

            var body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getPage()).isEqualTo(1);
            assertThat(body.getSize()).isEqualTo(5);
            assertThat(body.getTotalElements()).isEqualTo(12);
            assertThat(body.getTotalPages()).isEqualTo(3);
            assertThat(body.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("maps SyncRun entity to DTO with all fields")
        void mapsRunEntityToDto() {
            var run = makeRun("run-123", pl.qprogramming.calendarsync.entity.SyncRunStatus.FAILED);
            var page = new PageImpl<>(List.of(run), PageRequest.of(0, 20), 1);
            when(logService.getPagedRuns(0, 20, null)).thenReturn(page);

            var response = controller.getLogs(null, null, null);

            var dto = response.getBody().getContent().get(0);
            assertThat(dto.getId()).isEqualTo("run-123");
            assertThat(dto.getStatus()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(dto.getCreated()).isEqualTo(1);
            assertThat(dto.getUpdated()).isEqualTo(2);
            assertThat(dto.getDeleted()).isEqualTo(3);
            assertThat(dto.getMessage()).isEqualTo("test");
        }

        @Test
        @DisplayName("handles null entity status gracefully")
        void handlesNullEntityStatus() {
            var run = new SyncRunEntity();
            run.setId("r-null");
            run.setCreated(0); run.setUpdated(0); run.setDeleted(0);
            // status = null
            var page = new PageImpl<>(List.of(run), PageRequest.of(0, 20), 1);
            when(logService.getPagedRuns(0, 20, null)).thenReturn(page);

            var response = controller.getLogs(null, null, null);

            assertThat(response.getBody().getContent().get(0).getStatus()).isNull();
        }
    }

    // ── getLogDetails ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getLogDetails")
    class GetLogDetails {

        @Test
        @DisplayName("returns 200 with run and entries when run found")
        void returns200WithDetails_WhenRunFound() {
            var run = makeRun("run-x", pl.qprogramming.calendarsync.entity.SyncRunStatus.SUCCESS);

            var entry = new SyncLogEntryEntity();
            entry.setRunId("run-x");
            entry.setLevel(LogLevel.INFO);
            entry.setMessage("Sync started");
            entry.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));

            when(logService.getRun("run-x")).thenReturn(Optional.of(run));
            when(logService.getEntries("run-x")).thenReturn(List.of(entry));

            var response = controller.getLogDetails("run-x");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            var body = response.getBody();
            assertThat(body.getRun().getId()).isEqualTo("run-x");
            assertThat(body.getRun().getStatus()).isEqualTo(SyncRunStatus.SUCCESS);
            assertThat(body.getEntries()).hasSize(1);
            assertThat(body.getEntries().get(0).getMessage()).isEqualTo("Sync started");
            assertThat(body.getEntries().get(0).getLevel()).isEqualTo(LogLevel.INFO);
        }

        @Test
        @DisplayName("returns 404 when run not found")
        void returns404_WhenRunNotFound() {
            when(logService.getRun("nonexistent")).thenReturn(Optional.empty());

            var response = controller.getLogDetails("nonexistent");

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("returns empty entries list when no log entries exist")
        void returnsEmptyEntries_WhenNone() {
            var run = makeRun("run-y", pl.qprogramming.calendarsync.entity.SyncRunStatus.RUNNING);
            when(logService.getRun("run-y")).thenReturn(Optional.of(run));
            when(logService.getEntries("run-y")).thenReturn(List.of());

            var response = controller.getLogDetails("run-y");

            assertThat(response.getBody().getEntries()).isEmpty();
        }
    }
}
