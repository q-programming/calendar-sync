package pl.qprogramming.calendarsync.port;

import java.util.List;
import java.util.Map;

public interface GoogleCalendarPort {
    List<CalendarRef> listCalendars();
    List<GoogleEvent> readEvents(String calendarId, DateRange range);

    /**
     * Apply all changes in a single client session.
     * @param toCreate    Outlook events to insert into Google
     * @param toUpdate    mapping of Google event ID → Outlook event (for updates)
     * @param toDeleteIds Google event IDs to delete
     */
    BatchWriteResult batchWrite(String calendarId,
                                List<OutlookEvent> toCreate,
                                Map<String, OutlookEvent> toUpdate,
                                List<String> toDeleteIds,
                                boolean syncColorLabels);
}
