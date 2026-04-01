package pl.qprogramming.calendarsync.port;

import java.security.Principal;
import java.util.List;

public interface GoogleCalendarPort {
    List<CalendarRef> listCalendars(Principal principal);
    List<GoogleEvent> readEvents(Principal principal, String calendarId, DateRange range);
    GoogleEvent upsertEvent(Principal principal, String calendarId, OutlookEvent event, String existingGoogleEventId);
    void deleteEvent(Principal principal, String calendarId, String eventId);
}
