package pl.qprogramming.calendarsync.model;

/**
 * Lightweight reference to a calendar returned by either the Outlook or Google calendar listing.
 *
 * @param id       provider-specific identifier (Outlook: folder descriptor node id; Google: calendar API id)
 * @param name     human-readable display name
 * @param timeZone IANA time-zone id associated with the calendar, or {@code null} if not available
 * @param color    background colour hex string (Google only), or {@code null}
 */
public record CalendarRef(String id, String name, String timeZone, String color) {}

