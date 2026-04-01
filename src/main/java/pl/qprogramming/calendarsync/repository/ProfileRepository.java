package pl.qprogramming.calendarsync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.qprogramming.calendarsync.entity.ProfileEntity;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {}
