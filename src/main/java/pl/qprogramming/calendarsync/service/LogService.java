package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.SyncLogEntryEntity;
import pl.qprogramming.calendarsync.entity.SyncRunEntity;
import pl.qprogramming.calendarsync.repository.SyncLogEntryRepository;
import pl.qprogramming.calendarsync.repository.SyncRunRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LogService {

    private final SyncRunRepository syncRunRepository;
    private final SyncLogEntryRepository syncLogEntryRepository;

    public Page<SyncRunEntity> getPagedRuns(int page, int size, String status) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        if (status != null && !status.isBlank()) {
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
}
