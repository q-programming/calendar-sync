package pl.qprogramming.calendarsync.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.api.SyncApiDelegate;
import pl.qprogramming.calendarsync.dto.SyncRun;
import pl.qprogramming.calendarsync.dto.SyncRunStatus;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.service.SyncService;

@Component
@RequiredArgsConstructor
public class SyncController implements SyncApiDelegate {

    private final SyncService syncService;

    @Override
    public ResponseEntity<SyncRun> triggerSync() {
        SyncRunEntity run = syncService.runSync();
        return ResponseEntity.ok(toDto(run));
    }

    private SyncRun toDto(SyncRunEntity e) {
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
}
