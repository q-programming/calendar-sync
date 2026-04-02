package pl.qprogramming.calendarsync.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.entity.SyncRunStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface SyncRunRepository extends JpaRepository<SyncRunEntity, String> {
    Page<SyncRunEntity> findByStatus(SyncRunStatus status, Pageable pageable);
    List<SyncRunEntity> findAllByStatus(SyncRunStatus status);
    List<SyncRunEntity> findAllByStartedAtBefore(OffsetDateTime  startedAt);
}
