package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.dto.LogLevel;
import pl.qprogramming.calendarsync.entity.SyncLogEntryEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncRunStatus;
import pl.qprogramming.calendarsync.repository.SyncLogEntryRepository;
import pl.qprogramming.calendarsync.repository.SyncRunRepository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

/**
 * Dual-purpose logging service that writes both to SLF4J and to the database.
 *
 * <p>Every sync run gets a {@link pl.qprogramming.calendarsync.entity.SyncRunEntity} header row
 * and a sequence of {@link pl.qprogramming.calendarsync.entity.SyncLogEntryEntity} detail rows.
 * Callers wrap their work in {@link #logAroundRun} to establish a per-thread run context;
 * all subsequent {@code info/debug/warn/error} calls inside that block are automatically
 * persisted under the active run id.
 *
 * <p>Thread isolation is achieved via {@link ThreadLocal} so that
 * {@code @Async}-executed sync threads don't share run state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogService {

    private final SyncRunRepository syncRunRepository;
    private final SyncLogEntryRepository syncLogEntryRepository;

    // ── Run context (ThreadLocal so @Async threads are isolated) ─────────────────

    private record RunContext(String runId, boolean debugEnabled) {
    }

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

    /**
     * Logs and persists an INFO-level message for the active run.
     *
     * @param fmt  {@link String#format}-style format string
     * @param args format arguments
     */
    public void info(String fmt, Object... args) {
        String msg = format(fmt, args);
        persist(LogLevel.INFO, msg);
        log.info(msg);
    }

    /**
     * Logs and persists a DEBUG-level message only when debug logging is enabled for the run.
     * Has no effect if called outside a run context or if the run's {@code debugEnabled} flag is false.
     *
     * @param fmt  {@link String#format}-style format string
     * @param args format arguments
     */
    public void debug(String fmt, Object... args) {
        RunContext ctx = currentRun.get();
        if (ctx != null && ctx.debugEnabled()) {
            String msg = format(fmt, args);
            persist(LogLevel.DEBUG, msg);
            log.debug(msg);
        }
    }

    /**
     * Logs and persists a WARN-level message for the active run.
     *
     * @param fmt  {@link String#format}-style format string
     * @param args format arguments
     */
    public void warn(String fmt, Object... args) {
        String msg = format(fmt, args);
        persist(LogLevel.WARN, msg);
        log.warn(msg);
    }

    /**
     * Logs and persists an ERROR-level message for the active run.
     *
     * @param fmt  {@link String#format}-style format string
     * @param args format arguments
     */
    public void error(String fmt, Object... args) {
        String msg = format(fmt, args);
        persist(LogLevel.ERROR, msg);
        log.error(msg);
    }

    /**
     * Formats a message string using {@link String#format} when arguments are present,
     * or returns the raw format string directly when there are none (avoids unnecessary allocation).
     *
     * @param fmt  format string
     * @param args optional format arguments
     * @return formatted message
     */
    private String format(String fmt, Object[] args) {
        return args.length == 0 ? fmt : String.format(fmt, args);
    }

    /**
     * Persists a log entry for the currently active run context.
     * Logs a warning to SLF4J and discards the message if called outside a run context,
     * since there is no run id to associate the entry with.
     *
     * @param level log level for the entry
     * @param msg   fully formatted message text
     */
    private void persist(LogLevel level, String msg) {
        RunContext ctx = currentRun.get();
        if (ctx == null) {
            log.warn("LogService called outside of a run context — message lost: {}", msg);
            return;
        }
        SyncLogEntryEntity entry = new SyncLogEntryEntity();
        entry.setRunId(ctx.runId());
        entry.setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        entry.setLevel(level);
        entry.setMessage(msg);
        syncLogEntryRepository.save(entry);
    }

    // ── Query / persistence ──────────────────────────────────────────────────────

    /**
     * Returns a page of sync run headers, ordered by start time descending.
     *
     * @param page   zero-based page number
     * @param size   maximum number of rows per page
     * @param status optional status filter; pass {@code null} to return all statuses
     * @return a page of {@link pl.qprogramming.calendarsync.entity.SyncRunEntity} records
     */
    public Page<SyncRunEntity> getPagedRuns(int page, int size, SyncRunStatus status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        if (status != null) {
            return syncRunRepository.findByStatus(status, pageable);
        }
        return syncRunRepository.findAll(pageable);
    }

    /**
     * Finds a single sync run by its id.
     *
     * @param runId UUID string identifying the run
     * @return an {@link Optional} containing the run if found
     */
    public Optional<SyncRunEntity> getRun(String runId) {
        return syncRunRepository.findById(runId);
    }

    /**
     * Returns all log entries for a specific run, sorted by timestamp ascending.
     *
     * @param runId UUID string identifying the run
     * @return ordered list of {@link pl.qprogramming.calendarsync.entity.SyncLogEntryEntity}
     */
    public List<SyncLogEntryEntity> getEntries(String runId) {
        return syncLogEntryRepository.findByRunIdOrderByTimestampAsc(runId);
    }

    /**
     * Persists (creates or updates) a sync run header record.
     *
     * @param run the run entity to save
     * @return the saved (and potentially ID-assigned) entity
     */
    public SyncRunEntity saveRun(SyncRunEntity run) {
        return syncRunRepository.save(run);
    }

    /**
     * Persists a single log entry row directly, bypassing the thread-local run context.
     * Intended for low-level housekeeping writes (e.g., marking a stale run as failed).
     *
     * @param entry the entry to persist
     */
    public void saveEntry(SyncLogEntryEntity entry) {
        syncLogEntryRepository.save(entry);
    }

    /**
     * Returns all sync runs currently in {@link SyncRunStatus#RUNNING} state.
     * Used on startup to detect runs that were interrupted by an application restart.
     *
     * @return list of runs that did not finish cleanly
     */
    public List<SyncRunEntity> getRunningRuns() {
        return syncRunRepository.findAllByStatus(SyncRunStatus.RUNNING);
    }

    /**
     * Deletes sync runs (and their associated log entries) that started more than 30 days ago.
     * Intended to be called periodically to keep the database from growing unbounded.
     */
    public void removeOldEntries() {
        syncRunRepository.findAllByStartedAtBefore(OffsetDateTime.now(ZoneOffset.UTC).minusDays(30)).forEach(run -> {
            syncLogEntryRepository.deleteAll(syncLogEntryRepository.findByRunIdOrderByTimestampAsc(run.getId()));
            syncRunRepository.delete(run);
        });
    }

    /**
     * Transitions any {@link SyncRunStatus#RUNNING} runs to {@link SyncRunStatus#FAILED}.
     *
     * <p>Called on application startup. If the JVM was killed while a sync was active,
     * the run row is left in RUNNING state forever. This method detects those orphaned runs
     * and marks them as failed, adding a log entry to explain the interruption.
     */
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
            entry.setLevel(LogLevel.WARN);
            entry.setMessage("Sync run was interrupted by an application restart and did not complete normally");
            saveEntry(entry);
            log.warn("Marked sync run '{}' (started {}) as FAILED", run.getId(), run.getStartedAt());
        }
    }
}
