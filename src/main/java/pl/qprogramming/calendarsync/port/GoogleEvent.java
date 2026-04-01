package pl.qprogramming.calendarsync.port;

import java.time.ZonedDateTime;

public record GoogleEvent(
    String id,
    String outlookId,
    String summary,
    String description,
    String location,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean allDay
) {}
