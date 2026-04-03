package pl.qprogramming.calendarsync.entity;

import jakarta.persistence.*;
import lombok.*;
import pl.qprogramming.calendarsync.dto.LogLevel;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sync_log_entry")
@Getter @Setter @NoArgsConstructor
public class SyncLogEntryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String runId;
    @Column(name = "entry_timestamp")
    private OffsetDateTime timestamp;
    @Enumerated(EnumType.STRING)
    private LogLevel level;
    @Column(length = 4000)
    private String message;
}
