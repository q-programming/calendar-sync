package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncRunStatus;
import pl.qprogramming.calendarsync.model.DateRange;
import pl.qprogramming.calendarsync.service.google.BatchWriteResult;
import pl.qprogramming.calendarsync.service.google.GoogleCalendarService;
import pl.qprogramming.calendarsync.service.google.GoogleEvent;
import pl.qprogramming.calendarsync.service.outlook.OutlookCalendarService;
import pl.qprogramming.calendarsync.service.outlook.OutlookEvent;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Orchestrates the one-way Outlook → Google Calendar synchronisation process.
 *
 * <p>The sync pipeline runs as follows:
 * <ol>
 *   <li>Read all Outlook appointments in the configured date window.</li>
 *   <li>Read all Google events previously created by this sync (identified by a private marker).</li>
 *   <li>Diff the two sets: classify each Outlook event as CREATE, UPDATE, or SKIP.</li>
 *   <li>Mark Google events with no matching Outlook counterpart as DELETE.</li>
 *   <li>Apply all changes to Google Calendar via a single batch write.</li>
 * </ol>
 *
 * <p>Concurrency: only one sync may run at a time. An {@link java.util.concurrent.atomic.AtomicBoolean}
 * guards the {@link #runSync()} entry point; additional calls while a sync is active are rejected
 * immediately with a {@link SyncRunStatus#FAILED} skipped-run record.
 *
 * <p>The heavy lifting ({@link #executeSync}) is dispatched via Spring's {@code @Async}
 * so that the HTTP request creating the run returns immediately with the run id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final OutlookCalendarService outlookService;
    private final GoogleCalendarService googleService;
    private final ProfileService profileService;
    private final SettingsService settingsService;
    private final LogService logService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Returns {@code true} if a sync is currently in progress on any thread.
     *
     * @return {@code true} while {@link #executeSync} has not yet completed
     */
    public boolean isRunning() {
        return running.get();
    }

    /** Creates the run record immediately and fires the actual work asynchronously. */
    public SyncRunEntity runSync() {
        if (!running.compareAndSet(false, true)) {
            SyncRunEntity skipped = new SyncRunEntity();
            skipped.setId(UUID.randomUUID().toString());
            skipped.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
            skipped.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
            skipped.setStatus(SyncRunStatus.FAILED);
            skipped.setMessage("Another sync is already running");
            skipped.setCreated(0); skipped.setUpdated(0); skipped.setDeleted(0);
            logService.saveRun(skipped);
            return skipped;
        }
        SyncRunEntity run = new SyncRunEntity();
        run.setId(UUID.randomUUID().toString());
        run.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        run.setStatus(SyncRunStatus.RUNNING);
        run.setCreated(0); run.setUpdated(0); run.setDeleted(0);
        logService.saveRun(run);
        executeSync(run);
        return run;
    }

    /**
     * Async wrapper that delegates to {@link #doSync} and releases the running flag when done.
     * Annotated with {@code @Async} so it executes on a task-executor thread, not the caller's.
     *
     * @param run the pre-persisted run entity to update with results
     */
    @Async
    public void executeSync(SyncRunEntity run) {
        try {
            doSync(run);
        } finally {
            running.set(false);
        }
    }

    /**
     * Core sync logic: reads events from both calendars, computes the diff, writes changes to Google.
     * Updates {@code run} with final counts and status before returning.
     *
     * @param run mutable run entity that tracks progress and is saved at the end
     */
    private void doSync(SyncRunEntity run) {
        var settings = settingsService.getOrCreate();
        var profile  = profileService.getOrCreate();

        logService.logAroundRun(run.getId(), settings.isDebugLogging(), () -> {
            try {
                String outlookPath       = profile.getOutlookProfilePath();
                String outlookCalendarId = profile.getOutlookCalendarId();
                String googleCalendarId  = profile.getGoogleCalendarId();

                if (outlookPath == null || outlookPath.isBlank())
                    throw new IllegalStateException("Outlook profile path is not configured");
                if (outlookCalendarId == null || outlookCalendarId.isBlank())
                    throw new IllegalStateException("Outlook calendar is not selected");
                if (googleCalendarId == null || googleCalendarId.isBlank())
                    throw new IllegalStateException("Google calendar is not selected");

                ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
                DateRange range = new DateRange(
                        now.minusDays(settings.getDaysPast()),
                        now.plusDays(settings.getDaysFuture()));

                logService.info("Starting sync. Range: %s to %s", formatDate(range.from()), formatDate(range.to()));

                List<OutlookEvent> outlookEvents = outlookService.readEvents(outlookPath, outlookCalendarId, range);
                logService.info("Read %d Outlook events", outlookEvents.size());
                for (OutlookEvent oe : outlookEvents) {
                    logService.debug("  [OUTLOOK] %s | %s → %s | allDay=%s | location=%s",
                            oe.subject(), formatEventTime(oe.start()), formatEventTime(oe.end()),
                            oe.allDay(), oe.location());
                }

                List<GoogleEvent> googleEvents = googleService.readEvents(googleCalendarId, range);
                logService.info("Read %d Google (sync-managed) events", googleEvents.size());
                for (GoogleEvent ge : googleEvents) {
                    logService.debug("  [GOOGLE] %s | %s → %s | outlookId=%s",
                            ge.summary(), formatEventTime(ge.start()), formatEventTime(ge.end()), ge.outlookId());
                }

                Map<String, GoogleEvent> googleByOutlookId = googleEvents.stream()
                        .filter(e -> e.outlookId() != null)
                        .collect(Collectors.toMap(GoogleEvent::outlookId, e -> e, (a, b) -> a));

                Map<String, OutlookEvent> outlookById = outlookEvents.stream()
                        .collect(Collectors.toMap(OutlookEvent::id, e -> e, (a, b) -> a));

                int created, updated, deleted;

                List<OutlookEvent> toCreate = new ArrayList<>();
                Map<String, OutlookEvent> toUpdate = new java.util.LinkedHashMap<>();
                List<String> toDeleteIds = new ArrayList<>();

                for (OutlookEvent oe : outlookEvents) {
                    GoogleEvent existing = googleByOutlookId.get(oe.id());
                    if (existing == null) {
                        logService.info("  CREATE \"%s\" — %s%s",
                                oe.subject(), formatEventTime(oe.start()),
                                oe.location() != null && !oe.location().isBlank() ? " @ " + oe.location() : "");
                        toCreate.add(oe);
                    } else if (isChanged(oe, existing)) {
                        logService.info("  UPDATE \"%s\" — was %s, now %s%s",
                                oe.subject(), formatEventTime(existing.start()), formatEventTime(oe.start()),
                                oe.location() != null && !oe.location().isBlank() ? " @ " + oe.location() : "");
                        toUpdate.put(existing.id(), oe);
                    } else {
                        logService.debug("  [SKIP] No changes: \"%s\" — %s%s",
                                oe.subject(), formatEventTime(oe.start()),
                                oe.location() != null && !oe.location().isBlank() ? " @ " + oe.location() : "");
                    }
                }

                for (GoogleEvent ge : googleEvents) {
                    if (ge.outlookId() != null && !outlookById.containsKey(ge.outlookId())) {
                        logService.info("  DELETE \"%s\" — %s (removed from Outlook)",
                                ge.summary(), formatEventTime(ge.start()));
                        toDeleteIds.add(ge.id());
                    }
                }

                BatchWriteResult writeResult = googleService.batchWrite(
                        googleCalendarId, toCreate, toUpdate, toDeleteIds, settings.isSyncColorLabels());
                created = writeResult.created();
                updated = writeResult.updated();
                deleted = writeResult.deleted();
                if (writeResult.failed() > 0) {
                    logService.warn("%d operations failed during batch write", writeResult.failed());
                }

                run.setCreated(created);
                run.setUpdated(updated);
                run.setDeleted(deleted);
                run.setStatus(SyncRunStatus.SUCCESS);
                run.setMessage(String.format("Sync: +%d ~%d -%d", created, updated, deleted));
                logService.info("Run complete: create=%d, update=%d, delete=%d", created, updated, deleted);

            } catch (Exception e) {
                log.error("Sync failed", e);
                if (isGoogleTokenExpired(e)) {
                    run.setStatus(SyncRunStatus.GOOGLE_TOKEN_EXPIRED);
                    run.setMessage("Google token has expired or been revoked. Please reconnect your Google account.");
                    logService.error("Sync failed: Google token expired or revoked");
                } else {
                    run.setStatus(SyncRunStatus.FAILED);
                    run.setMessage(e.getMessage());
                    logService.error("Sync failed: %s", e.getMessage());
                }
            } finally {
                run.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
                logService.saveRun(run);
            }
        });
    }

    /**
     * Determines whether an Outlook event has changed relative to its previously synced Google counterpart.
     *
     * <p>Fields compared: subject, location, start/end time. For all-day events only the local
     * date is compared (time components are timezone-offset artefacts and must be ignored).
     * For timed events the comparison is instant-based so zone-id differences don't produce
     * false positives.
     *
     * @param oe the current Outlook appointment
     * @param ge the existing Google event mapped from the same Outlook id
     * @return {@code true} if any tracked field has changed and the Google event should be updated
     */
    boolean isChanged(OutlookEvent oe, GoogleEvent ge) {
        if (!eq(oe.subject(), ge.summary())) return true;
        if (!eqStr(oe.location(), ge.location())) return true;
        // All-day events: compare date only — times are arbitrary (timezone-offset artifacts)
        if (oe.allDay() || ge.allDay()) {
            return !eqDate(oe.start(), ge.start()) || !eqDate(oe.end(), ge.end());
        }
        return !eqInstant(oe.start(), ge.start()) || !eqInstant(oe.end(), ge.end());
    }

    /**
     * Null-safe equality check for arbitrary objects.
     *
     * @param a first value
     * @param b second value
     * @return {@code true} if both are {@code null} or {@code a.equals(b)}
     */
    private boolean eq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    /** Compare strings treating null and "" as equal (Google returns null for empty location). */
    private boolean eqStr(String a, String b) {
        String na = (a == null || a.isBlank()) ? null : a;
        String nb = (b == null || b.isBlank()) ? null : b;
        return java.util.Objects.equals(na, nb);
    }

    /** Compare ZonedDateTimes by local date only (for all-day events where time is meaningless). */
    private boolean eqDate(java.time.ZonedDateTime a, java.time.ZonedDateTime b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.toLocalDate().equals(b.toLocalDate());
    }

    /** Compare ZonedDateTimes by instant (ignores zone ID differences like Europe/Warsaw vs +02:00). */
    boolean eqInstant(java.time.ZonedDateTime a, java.time.ZonedDateTime b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.toInstant().equals(b.toInstant());
    }

    /** Format ZonedDateTime as "Fri Apr 3, 09:00" in system timezone — readable in user logs. */
    private String formatEventTime(java.time.ZonedDateTime zdt) {
        if (zdt == null) return "?";
        java.time.ZonedDateTime local = zdt.withZoneSameInstant(java.time.ZoneId.systemDefault());
        return local.format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM d, HH:mm"));
    }

    /** Format ZonedDateTime as "Apr 3, 2026 09:00" — for range/summary messages. */
    private String formatDate(java.time.ZonedDateTime zdt) {
        if (zdt == null) return "?";
        return zdt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"));
    }

    /**
     * Returns {@code true} if the exception (or any cause in its chain) indicates
     * that the Google OAuth token has expired or been revoked ({@code invalid_grant}).
     */
    private boolean isGoogleTokenExpired(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && msg.contains("invalid_grant")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }
}
