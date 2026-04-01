package pl.qprogramming.calendarsync.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;

public interface SyncRunRepository extends JpaRepository<SyncRunEntity, String> {
    Page<SyncRunEntity> findByStatus(String status, Pageable pageable);
}
