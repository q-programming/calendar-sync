package pl.qprogramming.calendarsync.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LogService")
class LogServiceTest {

    @Mock
    SyncRunRepository syncRunRepository;

    @Mock
    SyncLogEntryRepository syncLogEntryRepository;

    @InjectMocks
    LogService logService;

    // ── logAroundRun ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logAroundRun")
    class LogAroundRun {

        @Test
        @DisplayName("executes the block")
        void executesBlock() {
            boolean[] ran = {false};
            logService.logAroundRun("run-1", false, () -> ran[0] = true);
            assertThat(ran[0]).isTrue();
        }

        @Test
        @DisplayName("sets context during execution and clears it after")
        void contextSetDuringAndClearedAfter() {
            when(syncLogEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            boolean[] savedInsideBlock = {false};

            logService.logAroundRun("run-ctx", false, () -> {
                // info() inside block should persist (context is set)
                logService.info("inside block");
                savedInsideBlock[0] = true;
            });

            assertThat(savedInsideBlock[0]).isTrue();
            // After the block, context is cleared; info() should NOT persist
            logService.info("after block");
            // save was called exactly once (inside block), not for the call after block
            verify(syncLogEntryRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("clears context even when block throws")
        void clearContextOnException() {
            assertThatCode(() ->
                logService.logAroundRun("run-err", false, () -> {
                    throw new RuntimeException("boom");
                })
            ).isInstanceOf(RuntimeException.class);

            // After exception, context is cleared — info should NOT persist
            logService.info("after throw");
            verify(syncLogEntryRepository, never()).save(any());
        }
    }

    // ── Logging methods inside a run context ─────────────────────────────────

    @Nested
    @DisplayName("inside run context")
    class InsideRunContext {

        @BeforeEach
        void startRun() {
            when(syncLogEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        @DisplayName("info() persists INFO entry with correct message")
        void infoPersistsEntry() {
            logService.logAroundRun("r1", false, () -> logService.info("hello %s", "world"));

            ArgumentCaptor<SyncLogEntryEntity> captor = ArgumentCaptor.forClass(SyncLogEntryEntity.class);
            verify(syncLogEntryRepository).save(captor.capture());
            assertThat(captor.getValue().getLevel()).isEqualTo(LogLevel.INFO);
            assertThat(captor.getValue().getMessage()).isEqualTo("hello world");
            assertThat(captor.getValue().getRunId()).isEqualTo("r1");
        }

        @Test
        @DisplayName("warn() persists WARN entry")
        void warnPersistsEntry() {
            logService.logAroundRun("r2", false, () -> logService.warn("something wrong"));

            ArgumentCaptor<SyncLogEntryEntity> captor = ArgumentCaptor.forClass(SyncLogEntryEntity.class);
            verify(syncLogEntryRepository).save(captor.capture());
            assertThat(captor.getValue().getLevel()).isEqualTo(LogLevel.WARN);
        }

        @Test
        @DisplayName("error() persists ERROR entry")
        void errorPersistsEntry() {
            logService.logAroundRun("r3", false, () -> logService.error("broken: %s", "cause"));

            ArgumentCaptor<SyncLogEntryEntity> captor = ArgumentCaptor.forClass(SyncLogEntryEntity.class);
            verify(syncLogEntryRepository).save(captor.capture());
            assertThat(captor.getValue().getLevel()).isEqualTo(LogLevel.ERROR);
            assertThat(captor.getValue().getMessage()).isEqualTo("broken: cause");
        }

        @Test
        @DisplayName("debug() persists DEBUG when debugEnabled=true")
        void debugPersistedWhenEnabled() {
            logService.logAroundRun("r4", true, () -> logService.debug("verbose"));

            ArgumentCaptor<SyncLogEntryEntity> captor = ArgumentCaptor.forClass(SyncLogEntryEntity.class);
            verify(syncLogEntryRepository).save(captor.capture());
            assertThat(captor.getValue().getLevel()).isEqualTo(LogLevel.DEBUG);
        }

        @Test
        @DisplayName("debug() does NOT persist when debugEnabled=false")
        void debugSuppressedWhenDisabled() {
            logService.logAroundRun("r5", false, () -> logService.debug("quiet"));

            verify(syncLogEntryRepository, never()).save(any());
        }
    }

    // ── Logging methods outside a run context ─────────────────────────────────

    @Nested
    @DisplayName("outside run context")
    class OutsideRunContext {

        @Test
        @DisplayName("info() outside context does NOT persist")
        void infoOutsideContextDoesNotPersist() {
            logService.info("orphan info");
            verify(syncLogEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("warn() outside context does NOT persist")
        void warnOutsideContextDoesNotPersist() {
            logService.warn("orphan warn");
            verify(syncLogEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("error() outside context does NOT persist")
        void errorOutsideContextDoesNotPersist() {
            logService.error("orphan error");
            verify(syncLogEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("debug() outside context does NOT persist")
        void debugOutsideContextDoesNotPersist() {
            logService.debug("orphan debug");
            verify(syncLogEntryRepository, never()).save(any());
        }
    }

    // ── Query methods ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("query methods")
    class QueryMethods {

        @Test
        @DisplayName("getPagedRuns with null status calls findAll")
        void getPagedRuns_nullStatus_callsFindAll() {
            var page = new PageImpl<SyncRunEntity>(List.of());
            when(syncRunRepository.findAll(any(Pageable.class))).thenReturn(page);

            var result = logService.getPagedRuns(0, 10, null);

            assertThat(result).isEqualTo(page);
            verify(syncRunRepository).findAll(any(Pageable.class));
            verify(syncRunRepository, never()).findByStatus(any(), any());
        }

        @Test
        @DisplayName("getPagedRuns with non-null status calls findByStatus")
        void getPagedRuns_withStatus_callsFindByStatus() {
            var page = new PageImpl<SyncRunEntity>(List.of());
            when(syncRunRepository.findByStatus(any(), any(Pageable.class))).thenReturn(page);

            var result = logService.getPagedRuns(0, 10, SyncRunStatus.RUNNING);

            assertThat(result).isEqualTo(page);
            verify(syncRunRepository).findByStatus(eq(SyncRunStatus.RUNNING), any(Pageable.class));
            verify(syncRunRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("getRun delegates to findById")
        void getRun_delegatesToFindById() {
            var run = new SyncRunEntity();
            run.setId("run-x");
            when(syncRunRepository.findById("run-x")).thenReturn(Optional.of(run));

            var result = logService.getRun("run-x");

            assertThat(result).isPresent().contains(run);
        }

        @Test
        @DisplayName("getEntries delegates to findByRunId")
        void getEntries_delegatesToRepository() {
            var entry = new SyncLogEntryEntity();
            when(syncLogEntryRepository.findByRunIdOrderByTimestampAsc("r1")).thenReturn(List.of(entry));

            var result = logService.getEntries("r1");

            assertThat(result).containsExactly(entry);
        }

        @Test
        @DisplayName("saveRun delegates to syncRunRepository.save")
        void saveRun_delegates() {
            var run = new SyncRunEntity();
            run.setId("saved");
            when(syncRunRepository.save(run)).thenReturn(run);

            var result = logService.saveRun(run);

            assertThat(result.getId()).isEqualTo("saved");
            verify(syncRunRepository).save(run);
        }
    }

    // ── Housekeeping ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeOldEntries")
    class RemoveOldEntries {

        @Test
        @DisplayName("deletes entries and runs older than 30 days")
        void deletesOldRunsAndEntries() {
            var oldRun = new SyncRunEntity();
            oldRun.setId("old-run");
            oldRun.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(31));

            when(syncRunRepository.findAllByOrderByStartedAtDesc()).thenReturn(List.of(oldRun));

            logService.removeOldEntries();

            verify(syncLogEntryRepository).deleteAllByRunIdIn(List.of("old-run"));
            verify(syncRunRepository).deleteAllByIdIn(List.of("old-run"));
        }

        @Test
        @DisplayName("deletes runs beyond the most recent 100")
        void deletesRunsBeyondMaxLimit() {
            List<SyncRunEntity> all = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                var filler = new SyncRunEntity();
                filler.setId("filler-" + i);
                filler.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
                all.add(filler);
            }
            var excess = new SyncRunEntity();
            excess.setId("excess");
            excess.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(2));
            all.add(excess);

            when(syncRunRepository.findAllByOrderByStartedAtDesc()).thenReturn(all);

            logService.removeOldEntries();

            verify(syncLogEntryRepository).deleteAllByRunIdIn(List.of("excess"));
            verify(syncRunRepository).deleteAllByIdIn(List.of("excess"));
        }

        @Test
        @DisplayName("does nothing when no old runs exist")
        void doesNothingWhenNoOldRuns() {
            when(syncRunRepository.findAllByOrderByStartedAtDesc()).thenReturn(List.of());

            logService.removeOldEntries();

            verify(syncLogEntryRepository, never()).deleteAllByRunIdIn(any());
            verify(syncRunRepository, never()).deleteAllByIdIn(any());
        }
    }

    @Nested
    @DisplayName("failStaleSyncRuns")
    class FailStaleSyncRuns {

        @Test
        @DisplayName("returns early when no RUNNING runs")
        void returnsEarlyWhenNoStaleRuns() {
            when(syncRunRepository.findAllByStatus(SyncRunStatus.RUNNING)).thenReturn(List.of());

            logService.failStaleSyncRuns();

            verify(syncRunRepository, never()).save(any());
            verify(syncLogEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("marks stale RUNNING runs as FAILED with message and log entry")
        void marksStaleRunsAsFailed() {
            var stale = new SyncRunEntity();
            stale.setId("stale-run");
            stale.setStatus(SyncRunStatus.RUNNING);

            when(syncRunRepository.findAllByStatus(SyncRunStatus.RUNNING)).thenReturn(List.of(stale));
            when(syncRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(syncLogEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            logService.failStaleSyncRuns();

            ArgumentCaptor<SyncRunEntity> runCaptor = ArgumentCaptor.forClass(SyncRunEntity.class);
            verify(syncRunRepository).save(runCaptor.capture());
            assertThat(runCaptor.getValue().getStatus()).isEqualTo(SyncRunStatus.FAILED);
            assertThat(runCaptor.getValue().getFinishedAt()).isNotNull();
            assertThat(runCaptor.getValue().getMessage()).contains("restarted");

            ArgumentCaptor<SyncLogEntryEntity> entryCaptor = ArgumentCaptor.forClass(SyncLogEntryEntity.class);
            verify(syncLogEntryRepository).save(entryCaptor.capture());
            assertThat(entryCaptor.getValue().getRunId()).isEqualTo("stale-run");
            assertThat(entryCaptor.getValue().getLevel()).isEqualTo(LogLevel.WARN);
        }

        @Test
        @DisplayName("processes multiple stale runs")
        void processesMultipleStaleRuns() {
            var run1 = new SyncRunEntity();
            run1.setId("r1");
            run1.setStatus(SyncRunStatus.RUNNING);
            var run2 = new SyncRunEntity();
            run2.setId("r2");
            run2.setStatus(SyncRunStatus.RUNNING);

            when(syncRunRepository.findAllByStatus(SyncRunStatus.RUNNING)).thenReturn(List.of(run1, run2));
            when(syncRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(syncLogEntryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            logService.failStaleSyncRuns();

            verify(syncRunRepository, times(2)).save(any());
            verify(syncLogEntryRepository, times(2)).save(any());
        }
    }
}
