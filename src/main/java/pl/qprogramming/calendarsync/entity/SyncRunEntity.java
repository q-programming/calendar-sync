package pl.qprogramming.calendarsync.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sync_run")
@Getter @Setter @NoArgsConstructor
public class SyncRunEntity {
    @Id
    private String id;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30)")
    private SyncRunStatus status;
    private Integer created = 0;
    private Integer updated = 0;
    private Integer deleted = 0;
    @Column(length = 2000)
    private String message;
}
