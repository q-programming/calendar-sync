package pl.qprogramming.calendarsync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.qprogramming.calendarsync.entity.SyncLogEntryEntity;
import java.util.List;

public interface SyncLogEntryRepository extends JpaRepository<SyncLogEntryEntity, Long> {
    List<SyncLogEntryEntity> findByRunIdOrderByTimestampAsc(String runId);
}
