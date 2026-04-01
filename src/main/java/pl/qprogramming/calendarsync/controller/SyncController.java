package pl.qprogramming.calendarsync.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.api.SyncApiDelegate;
import pl.qprogramming.calendarsync.dto.SyncRun;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.service.SyncService;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class SyncController implements SyncApiDelegate {

    private final SyncService syncService;

    @Override
    public ResponseEntity<SyncRun> triggerSync() {
        Principal principal = SecurityContextHolder.getContext().getAuthentication();
        SyncRunEntity run = syncService.runSync(principal);
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
            try {
                dto.status(SyncRun.StatusEnum.fromValue(e.getStatus()));
            } catch (Exception ex) { /* ignore unknown status */ }
        }
        return dto;
    }
}
