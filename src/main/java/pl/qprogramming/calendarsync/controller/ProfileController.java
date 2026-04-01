package pl.qprogramming.calendarsync.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.api.ProfileApiDelegate;
import pl.qprogramming.calendarsync.dto.*;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.port.GoogleCalendarPort;
import pl.qprogramming.calendarsync.port.OutlookCalendarPort;
import pl.qprogramming.calendarsync.service.ProfileService;

import java.security.Principal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileController implements ProfileApiDelegate {

    private final ProfileService profileService;
    private final OutlookCalendarPort outlookPort;
    private final GoogleCalendarPort googlePort;

    @Override
    public ResponseEntity<Profile> getProfile() {
        Principal principal = getPrincipal();
        ProfileEntity entity = profileService.getOrCreate();
        boolean googleConnected = profileService.isGoogleConnected(principal);
        boolean outlookConnected = entity.getOutlookProfilePath() != null
                && !entity.getOutlookProfilePath().isBlank();

        Profile dto = new Profile()
                .googleConnected(googleConnected)
                .outlookConnected(outlookConnected)
                .outlookProfilePath(entity.getOutlookProfilePath())
                .outlookCalendarId(entity.getOutlookCalendarId())
                .outlookCalendarName(entity.getOutlookCalendarName())
                .googleCalendarId(entity.getGoogleCalendarId())
                .googleCalendarName(entity.getGoogleCalendarName());
        return ResponseEntity.ok(dto);
    }

    @Override
    public ResponseEntity<Void> connectOutlook(OutlookConnection outlookConnection) {
        try {
            profileService.connectOutlook(outlookConnection.getProfilePath());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Outlook profile path: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<List<CalendarRef>> getOutlookCalendars() {
        ProfileEntity profile = profileService.getOrCreate();
        if (profile.getOutlookProfilePath() == null || profile.getOutlookProfilePath().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            List<CalendarRef> refs = outlookPort.listCalendars(profile.getOutlookProfilePath()).stream()
                    .map(r -> new CalendarRef().id(r.id()).name(r.name())
                            .timeZone(r.timeZone()).color(r.color()))
                    .toList();
            return ResponseEntity.ok(refs);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<Void> setOutlookCalendar(CalendarSelection calendarSelection) {
        String calId = calendarSelection.getCalendarId();
        ProfileEntity profile = profileService.getOrCreate();
        String name = calId;
        if (profile.getOutlookProfilePath() != null) {
            name = outlookPort.listCalendars(profile.getOutlookProfilePath()).stream()
                    .filter(c -> c.id().equals(calId))
                    .findFirst()
                    .map(pl.qprogramming.calendarsync.port.CalendarRef::name)
                    .orElse(calId);
        }
        profileService.setOutlookCalendar(calId, name);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<CalendarRef>> getGoogleCalendars() {
        Principal principal = getPrincipal();
        if (!profileService.isGoogleConnected(principal)) {
            return ResponseEntity.status(401).build();
        }
        try {
            List<CalendarRef> refs = googlePort.listCalendars(principal).stream()
                    .map(r -> new CalendarRef().id(r.id()).name(r.name())
                            .timeZone(r.timeZone()).color(r.color()))
                    .toList();
            return ResponseEntity.ok(refs);
        } catch (Exception e) {
            log.error("Failed to list Google calendars", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Override
    public ResponseEntity<Void> setGoogleCalendar(CalendarSelection calendarSelection) {
        Principal principal = getPrincipal();
        String calId = calendarSelection.getCalendarId();
        String name = googlePort.listCalendars(principal).stream()
                .filter(c -> c.id().equals(calId))
                .findFirst()
                .map(pl.qprogramming.calendarsync.port.CalendarRef::name)
                .orElse(calId);
        profileService.setGoogleCalendar(calId, name);
        return ResponseEntity.noContent().build();
    }

    private Principal getPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
