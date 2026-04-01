package pl.qprogramming.calendarsync.port;

import java.time.ZonedDateTime;

public record DateRange(ZonedDateTime from, ZonedDateTime to) {}
