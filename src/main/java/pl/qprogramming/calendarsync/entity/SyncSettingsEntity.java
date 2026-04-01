package pl.qprogramming.calendarsync.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sync_settings")
@Getter @Setter @NoArgsConstructor
public class SyncSettingsEntity {
    @Id
    private Long id = 1L;
    private int frequencyMinutes = 60;
    private int daysPast = 7;
    private int daysFuture = 30;
    private boolean debugLogging = false;
}
