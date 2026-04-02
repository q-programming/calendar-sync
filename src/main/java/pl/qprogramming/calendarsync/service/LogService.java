package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.SyncLogEntryEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncRunStatus;
import pl.qprogramming.calendarsync.repository.SyncLogEntryRepository;
import pl.qprogramming.calendarsync.repository.SyncRunRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final SyncRunRepository syncRunRepository;
    private final SyncLogEntryRepository syncLogEntryRepository;

    // ── Run context (ThreadLocal so @Async threads are isolated) ─────────────────

    private record RunContext(String runId, boolean debugEnabled) {}

    private final ThreadLocal<RunContext> currentRun = new ThreadLocal<>();

    /**
     * Establishes a run context for the current thread, executes the block,
     * then clears the context. All logService.info/debug/warn/error calls
     * inside the block are automatically persisted as log entries for this run.
     */
    public void logAroundRun(String runId, boolean debugEnabled, Runnable block) {
        currentRun.set(new RunContext(runId, debugEnabled));
        try {
            block.run();
        } finally {
            currentRun.remove();
        }
    }

    public void info(String fmt, Object... args) {
        persist("INFO", fmt, args);
    }

    public void debug(String fmt, Object... args) {
        RunContext ctx = currentRun.get();
        if (ctx != null && ctx.debugEnabled()) {
            persist("DEBUG", fmt, args);
        }
    }

    public void warn(String fmt, Object... args) {
        persist("WARN", fmt, args);
    }

    public void error(String fmt, Object... args) {
        persist("ERROR", fmt, args);
    }

    private void persist(String level, String fmt, Object[] args) {
        RunContext ctx = currentRun.get();
        if (ctx == null) {
            log.warn("LogService.{}() called outside of a run context — message lost: {}", level.toLowerCase(), fmt);
            return;
        }
        String msg = args.length == 0 ? fmt : String.format(fmt, args);
        SyncLogEntryEntity entry = new SyncLogEntryEntity();
        entry.setRunId(ctx.runId());
        entry.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        entry.setLevel(level);
        entry.setMessage(msg);
        syncLogEntryRepository.save(entry);
    }

    // ── Query / persistence ──────────────────────────────────────────────────────

    public Page<SyncRunEntity> getPagedRuns(int page, int size, SyncRunStatus status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        if (status != null) {
            return syncRunRepository.findByStatus(status, pageable);
        }
        return syncRunRepository.findAll(pageable);
    }

    public Optional<SyncRunEntity> getRun(String runId) {
        return syncRunRepository.findById(runId);
    }

    public List<SyncLogEntryEntity> getEntries(String runId) {
        return syncLogEntryRepository.findByRunIdOrderByTimestampAsc(runId);
    }

    public SyncRunEntity saveRun(SyncRunEntity run) {
        return syncRunRepository.save(run);
    }

    public void saveEntry(SyncLogEntryEntity entry) {
        syncLogEntryRepository.save(entry);
    }

    public List<SyncRunEntity> getRunningRuns() {
        return syncRunRepository.findAllByStatus(SyncRunStatus.RUNNING);
    }

    public void removeOldEntries(){
        syncRunRepository.findAllByStartedAtBefore(OffsetDateTime.now(ZoneOffset.UTC).minusDays(30)).forEach(run -> {
            syncLogEntryRepository.deleteAll(syncLogEntryRepository.findByRunIdOrderByTimestampAsc(run.getId()));
            syncRunRepository.delete(run);
        });
    }

    public void failStaleSyncRuns() {
        List<SyncRunEntity> stale = getRunningRuns();
        if (stale.isEmpty()) {
            return;
        }
        log.warn("Found {} sync run(s) stuck in RUNNING state — marking as FAILED (app was restarted)", stale.size());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (SyncRunEntity run : stale) {
            run.setStatus(SyncRunStatus.FAILED);
            run.setFinishedAt(now);
            run.setMessage("Sync interrupted: application was restarted while this run was in progress");
            saveRun(run);
            SyncLogEntryEntity entry = new SyncLogEntryEntity();
            entry.setRunId(run.getId());
            entry.setTimestamp(now);
            entry.setLevel("WARN");
            entry.setMessage("Sync run was interrupted by an application restart and did not complete normally");
            saveEntry(entry);
            log.warn("Marked sync run '{}' (started {}) as FAILED", run.getId(), run.getStartedAt());
        }
    }
}
