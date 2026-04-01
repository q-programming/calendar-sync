package pl.qprogramming.calendarsync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.qprogramming.calendarsync.entity.SyncSettingsEntity;

public interface SyncSettingsRepository extends JpaRepository<SyncSettingsEntity, Long> {}
