package pl.qprogramming.calendarsync.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.api.ProfileApiDelegate;
import pl.qprogramming.calendarsync.dto.*;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.service.ProfileService;
import pl.qprogramming.calendarsync.service.SyncService;
import pl.qprogramming.calendarsync.service.google.GoogleCalendarService;
import pl.qprogramming.calendarsync.service.outlook.OutlookCalendarService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileController implements ProfileApiDelegate {

    private final ProfileService profileService;
    private final OutlookCalendarService outlookService;
    private final GoogleCalendarService googleService;
    private final SyncService syncService;

    @Override
    public ResponseEntity<Profile> getProfile() {
        ProfileEntity entity = profileService.getOrCreate();
        boolean googleConnected = profileService.isGoogleConnected();
        boolean outlookConnected = entity.getOutlookProfilePath() != null
                && !entity.getOutlookProfilePath().isBlank();

        Profile dto = new Profile(googleConnected, outlookConnected, syncService.isRunning())
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
            List<CalendarRef> refs = outlookService.listCalendars(profile.getOutlookProfilePath()).stream()
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
            name = outlookService.listCalendars(profile.getOutlookProfilePath()).stream()
                    .filter(c -> c.id().equals(calId)).findFirst()
                    .map(pl.qprogramming.calendarsync.model.CalendarRef::name)
                    .orElse(calId);
        }
        profileService.setOutlookCalendar(calId, name);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<CalendarRef>> getGoogleCalendars() {
        if (!profileService.isGoogleConnected()) {
            return ResponseEntity.status(401).build();
        }
        try {
            List<CalendarRef> refs = googleService.listCalendars().stream()
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
        String calId = calendarSelection.getCalendarId();
        String name = googleService.listCalendars().stream()
                .filter(c -> c.id().equals(calId)).findFirst()
                .map(pl.qprogramming.calendarsync.model.CalendarRef::name)
                .orElse(calId);
        profileService.setGoogleCalendar(calId, name);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> disconnectGoogle() {
        profileService.disconnectGoogle();
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> disconnectOutlook() {
        profileService.disconnectOutlook();
        return ResponseEntity.noContent().build();
    }
}
