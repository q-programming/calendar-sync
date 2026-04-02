package pl.qprogramming.calendarsync.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.api.LogsApiDelegate;
import pl.qprogramming.calendarsync.dto.*;
import pl.qprogramming.calendarsync.entity.SyncLogEntryEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.service.LogService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LogsController implements LogsApiDelegate {

    private final LogService logService;

    @Override
    public ResponseEntity<PagedSyncRuns> getLogs(Integer page, Integer size, SyncRunStatus status) {
        int p = page != null ? page : 0;
        int s = size != null ? size : 20;
        pl.qprogramming.calendarsync.entity.SyncRunStatus entityStatus =
                status != null ? pl.qprogramming.calendarsync.entity.SyncRunStatus.valueOf(status.name()) : null;
        Page<SyncRunEntity> result = logService.getPagedRuns(p, s, entityStatus);

        PagedSyncRuns paged = new PagedSyncRuns()
                .content(result.getContent().stream().map(this::toRunDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements((int) result.getTotalElements())
                .totalPages(result.getTotalPages());
        return ResponseEntity.ok(paged);
    }

    @Override
    public ResponseEntity<SyncRunDetails> getLogDetails(String runId) {
        return logService.getRun(runId)
                .map(run -> {
                    List<SyncLogEntryEntity> entries = logService.getEntries(runId);
                    SyncRunDetails details = new SyncRunDetails()
                            .run(toRunDto(run))
                            .entries(entries.stream().map(this::toEntryDto).toList());
                    return ResponseEntity.ok(details);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private SyncRun toRunDto(SyncRunEntity e) {
        SyncRun dto = new SyncRun()
                .id(e.getId())
                .startedAt(e.getStartedAt())
                .finishedAt(e.getFinishedAt())
                .created(e.getCreated())
                .updated(e.getUpdated())
                .deleted(e.getDeleted())
                .message(e.getMessage());
        if (e.getStatus() != null) {
            dto.status(SyncRunStatus.valueOf(e.getStatus().name()));
        }
        return dto;
    }

    private LogEntry toEntryDto(SyncLogEntryEntity e) {
        LogEntry dto = new LogEntry()
                .timestamp(e.getTimestamp())
                .message(e.getMessage());
        if (e.getLevel() != null) {
            try {
                dto.level(LogEntry.LevelEnum.fromValue(e.getLevel()));
            } catch (Exception ex) { /* ignore */ }
        }
        return dto;
    }
}
