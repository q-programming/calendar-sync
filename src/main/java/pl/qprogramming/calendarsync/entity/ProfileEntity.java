package pl.qprogramming.calendarsync.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profile_config")
@Getter @Setter @NoArgsConstructor
public class ProfileEntity {
    @Id
    private Long id = 1L;
    private String outlookProfilePath;
    private String outlookCalendarId;
    private String outlookCalendarName;
    private String googleCalendarId;
    private String googleCalendarName;
    private String googlePrincipalName;
}
