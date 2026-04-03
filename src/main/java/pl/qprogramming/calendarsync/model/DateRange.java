package pl.qprogramming.calendarsync.model;

import java.time.ZonedDateTime;

/**
 * Immutable time window passed to calendar read operations.
 *
 * @param from start of the range (inclusive)
 * @param to   end of the range (inclusive)
 */
public record DateRange(ZonedDateTime from, ZonedDateTime to) {}

