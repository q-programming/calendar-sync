package pl.qprogramming.calendarsync.service.outlook;

import java.time.ZonedDateTime;

/**
 * Normalised representation of a single Outlook appointment as returned by {@link OutlookCalendarService}.
 * All libpst types are translated into this record before leaving the service; no {@code com.pff.*}
 * objects are ever exposed to callers.
 *
 * @param id         stable identifier — folder descriptor node id for single events,
 *                   {@code nodeId_epochSecond} for recurring occurrences,
 *                   {@code nodeId_ex_origMin} for moved exceptions
 * @param subject    appointment title
 * @param body       plain-text body / notes
 * @param location   meeting location, or {@code null} / blank if not set
 * @param start      start time in the appointment's original timezone
 * @param end        end time in the appointment's original timezone
 * @param allDay     {@code true} for all-day events (time fields are timezone-offset artefacts and must be ignored)
 * @param colorIndex MS-OXOCAL {@code PidLidAppointmentColor} value (0 = none, 1–10 = Outlook colour categories)
 */
public record OutlookEvent(
        String id,
        String subject,
        String body,
        String location,
        ZonedDateTime start,
        ZonedDateTime end,
        boolean allDay,
        int colorIndex
) {}

