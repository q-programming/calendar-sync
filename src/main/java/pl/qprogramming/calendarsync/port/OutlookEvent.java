package pl.qprogramming.calendarsync.port;

import java.time.ZonedDateTime;

public record OutlookEvent(
    String id,
    String subject,
    String body,
    String location,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean allDay,
    int colorIndex   // MS-OXOCAL PidLidAppointmentColor (0=none, 1-10=Outlook colors)
) {}
