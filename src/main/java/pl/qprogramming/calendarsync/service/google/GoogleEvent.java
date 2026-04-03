package pl.qprogramming.calendarsync.service.google;

import java.time.ZonedDateTime;

/**
 * Normalised representation of a Google Calendar event as returned by {@link GoogleCalendarService}.
 * Only events previously created by this sync (identified by the private {@code source=outlook-sync}
 * extended property) are ever surfaced as this record.
 *
 * @param id          Google Calendar API event id
 * @param outlookId   the Outlook event id stored as a private extended property — the key used to
 *                    match Google events back to their Outlook counterparts during diff
 * @param summary     event title
 * @param description plain-text description
 * @param location    meeting location, or {@code null} if not set
 * @param start       start time
 * @param end         end time
 * @param allDay      {@code true} when the Google event has a {@code date} (not {@code dateTime}) field
 */
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

