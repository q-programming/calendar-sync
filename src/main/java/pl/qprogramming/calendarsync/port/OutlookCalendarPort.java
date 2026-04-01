package pl.qprogramming.calendarsync.port;

import java.util.List;

public interface OutlookCalendarPort {
    List<CalendarRef> listCalendars(String profilePath);
    List<OutlookEvent> readEvents(String profilePath, String calendarId, DateRange range);
}
