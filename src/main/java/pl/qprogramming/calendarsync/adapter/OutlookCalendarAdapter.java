package pl.qprogramming.calendarsync.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.port.*;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class OutlookCalendarAdapter implements OutlookCalendarPort {

    @Override
    public List<CalendarRef> listCalendars(String profilePath) {
        validateProfilePath(profilePath);
        log.warn("Outlook calendar listing is not yet implemented. Profile path: {}", profilePath);
        return List.of(new CalendarRef("default", "Default Calendar", "UTC", null));
    }

    @Override
    public List<OutlookEvent> readEvents(String profilePath, String calendarId, DateRange range) {
        validateProfilePath(profilePath);
        log.warn("Outlook event reading is not yet implemented (WSL stub). Returning empty list.");
        return List.of();
    }

    private void validateProfilePath(String profilePath) {
        if (profilePath == null || profilePath.isBlank()) {
            throw new IllegalArgumentException("Outlook profile path is not configured");
        }
        File file = new File(profilePath);
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(
                "Outlook profile path does not exist or is not readable: " + profilePath);
        }
    }
}
