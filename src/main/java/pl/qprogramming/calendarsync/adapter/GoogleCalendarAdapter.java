package pl.qprogramming.calendarsync.adapter;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Component;
import pl.qprogramming.calendarsync.port.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements GoogleCalendarPort {

    private static final String SYNC_SOURCE_KEY = "source";
    private static final String SYNC_SOURCE_VALUE = "outlook-sync";
    private static final String OUTLOOK_ID_KEY = "outlookId";

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final OAuth2AuthorizedClientService authorizedClientService;

    @Override
    public List<CalendarRef> listCalendars(Principal principal) {
        try {
            Calendar client = buildClient(principal);
            return client.calendarList().list().execute().getItems().stream()
                    .map(e -> new CalendarRef(e.getId(), e.getSummary(),
                            e.getTimeZone(), e.getBackgroundColor()))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list Google calendars", e);
            throw new RuntimeException("Failed to list Google calendars: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GoogleEvent> readEvents(Principal principal, String calendarId, DateRange range) {
        try {
            Calendar client = buildClient(principal);
            List<Event> items = client.events().list(calendarId)
                    .setTimeMin(new DateTime(range.from().toInstant().toEpochMilli()))
                    .setTimeMax(new DateTime(range.to().toInstant().toEpochMilli()))
                    .setSingleEvents(true)
                    .execute().getItems();

            return items.stream()
                    .filter(e -> {
                        var priv = e.getExtendedProperties();
                        return priv != null && priv.getPrivate() != null
                                && SYNC_SOURCE_VALUE.equals(priv.getPrivate().get(SYNC_SOURCE_KEY));
                    })
                    .map(this::toGoogleEvent)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to read Google events", e);
            throw new RuntimeException("Failed to read Google events: " + e.getMessage(), e);
        }
    }

    @Override
    public GoogleEvent upsertEvent(Principal principal, String calendarId,
                                   OutlookEvent outlookEvent, String existingGoogleEventId) {
        try {
            Calendar client = buildClient(principal);
            Event event = toGoogleApiEvent(outlookEvent);
            Event result = existingGoogleEventId != null
                    ? client.events().update(calendarId, existingGoogleEventId, event).execute()
                    : client.events().insert(calendarId, event).execute();
            return toGoogleEvent(result);
        } catch (Exception e) {
            log.error("Failed to upsert Google event", e);
            throw new RuntimeException("Failed to upsert event: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteEvent(Principal principal, String calendarId, String eventId) {
        try {
            Calendar client = buildClient(principal);
            client.events().delete(calendarId, eventId).execute();
        } catch (Exception e) {
            log.error("Failed to delete Google event {}", eventId, e);
            throw new RuntimeException("Failed to delete event: " + e.getMessage(), e);
        }
    }

    private Calendar buildClient(Principal principal) throws GeneralSecurityException, IOException {
        OAuth2AuthorizedClient ac = authorizedClientService.loadAuthorizedClient("google", principal.getName());
        if (ac == null || ac.getAccessToken() == null) {
            throw new IllegalStateException("No authorized Google client found for: " + principal.getName());
        }
        String token = ac.getAccessToken().getTokenValue();
        Instant expiresAt = ac.getAccessToken().getExpiresAt();
        String refresh = ac.getRefreshToken() != null ? ac.getRefreshToken().getTokenValue() : null;
        Date expDate = expiresAt != null ? Date.from(expiresAt) : new Date(System.currentTimeMillis() + 3600_000L);

        GoogleCredentials creds = refresh != null
                ? UserCredentials.newBuilder().setClientId(clientId).setClientSecret(clientSecret)
                        .setAccessToken(new AccessToken(token, expDate)).setRefreshToken(refresh).build()
                : GoogleCredentials.create(new AccessToken(token, expDate));

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds))
                .setApplicationName(applicationName)
                .build();
    }

    private Event toGoogleApiEvent(OutlookEvent src) {
        Event event = new Event();
        event.setSummary(src.subject());
        event.setDescription(src.body());
        event.setLocation(src.location());

        Map<String, String> priv = new HashMap<>();
        priv.put(SYNC_SOURCE_KEY, SYNC_SOURCE_VALUE);
        priv.put(OUTLOOK_ID_KEY, src.id());
        event.setExtendedProperties(new Event.ExtendedProperties().setPrivate(priv));

        ZoneId zone = src.start() != null ? src.start().getZone() : ZoneId.of("UTC");
        if (src.allDay()) {
            String d = src.start().toLocalDate().toString();
            event.setStart(new EventDateTime().setDate(new DateTime(d)));
            event.setEnd(new EventDateTime().setDate(new DateTime(src.end().toLocalDate().toString())));
        } else {
            event.setStart(new EventDateTime()
                    .setDateTime(new DateTime(src.start().toInstant().toEpochMilli()))
                    .setTimeZone(zone.getId()));
            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(src.end().toInstant().toEpochMilli()))
                    .setTimeZone(zone.getId()));
        }
        return event;
    }

    private GoogleEvent toGoogleEvent(Event e) {
        String outlookId = null;
        if (e.getExtendedProperties() != null && e.getExtendedProperties().getPrivate() != null) {
            outlookId = e.getExtendedProperties().getPrivate().get(OUTLOOK_ID_KEY);
        }
        return new GoogleEvent(e.getId(), outlookId, e.getSummary(), e.getDescription(),
                e.getLocation(), parseDateTime(e.getStart()), parseDateTime(e.getEnd()),
                e.getStart() != null && e.getStart().getDate() != null);
    }

    private ZonedDateTime parseDateTime(EventDateTime edt) {
        if (edt == null) return null;
        if (edt.getDateTime() != null) {
            ZoneId zone = edt.getTimeZone() != null ? ZoneId.of(edt.getTimeZone()) : ZoneId.of("UTC");
            return Instant.ofEpochMilli(edt.getDateTime().getValue()).atZone(zone);
        }
        if (edt.getDate() != null) {
            return java.time.LocalDate.parse(edt.getDate().toStringRfc3339().substring(0, 10))
                    .atStartOfDay(ZoneId.of("UTC"));
        }
        return null;
    }
}
