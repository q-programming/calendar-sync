package pl.qprogramming.calendarsync.adapter;

import com.google.api.client.googleapis.batch.BatchCallback;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
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
import pl.qprogramming.calendarsync.service.ProfileService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleCalendarAdapter implements GoogleCalendarPort {

    private static final String SYNC_SOURCE_KEY = "source";
    private static final String SYNC_SOURCE_VALUE = "outlook-sync";
    private static final String OUTLOOK_ID_KEY = "outlookId";

    /**
     * Outlook PidLidAppointmentColor (0x8214) → Google Calendar colorId.
     * Outlook: 0=None,1=Red,2=Blue,3=Green,4=Grey,5=Orange,6=Cyan,7=Olive,8=Purple,9=Teal,10=Yellow
     * Google colorIds: "1"=Lavender,"2"=Sage,"3"=Grape,"4"=Flamingo,"5"=Banana,"6"=Tangerine,
     *                  "7"=Peacock,"8"=Graphite,"9"=Blueberry,"10"=Basil,"11"=Tomato
     */
    private static final Map<Integer, String> OUTLOOK_TO_GOOGLE_COLOR = Map.of(
        1,  "11", // Red      → Tomato
        2,  "9",  // Blue     → Blueberry
        3,  "10", // Green    → Basil
        4,  "8",  // Grey     → Graphite
        5,  "6",  // Orange   → Tangerine
        6,  "7",  // Cyan     → Peacock
        7,  "2",  // Olive    → Sage
        8,  "3",  // Purple   → Grape
        9,  "1",  // Teal     → Lavender
        10, "5"   // Yellow   → Banana
    );

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ProfileService profileService;

    @Override
    public List<CalendarRef> listCalendars() {
        try {
            return buildClient().calendarList().list().execute().getItems().stream()
                    .map(e -> new CalendarRef(e.getId(), e.getSummary(), e.getTimeZone(), e.getBackgroundColor()))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to list Google calendars", e);
            throw new RuntimeException("Failed to list Google calendars: " + e.getMessage(), e);
        }
    }

    @Override
    public List<GoogleEvent> readEvents(String calendarId, DateRange range) {
        try {
            return buildClient().events().list(calendarId)
                    .setTimeMin(new DateTime(range.from().toInstant().toEpochMilli()))
                    .setTimeMax(new DateTime(range.to().toInstant().toEpochMilli()))
                    .setSingleEvents(true)
                    .execute().getItems().stream()
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

    /**
     * Build client once, convert all events, fire a single BatchRequest per chunk.
     * Google limits batch requests to 50 operations each, so we chunk automatically.
     */
    @Override
    public BatchWriteResult batchWrite(String calendarId,
                                       List<OutlookEvent> toCreate,
                                       Map<String, OutlookEvent> toUpdate,
                                       List<String> toDeleteIds,
                                       boolean syncColorLabels) {
        if (toCreate.isEmpty() && toUpdate.isEmpty() && toDeleteIds.isEmpty()) {
            return BatchWriteResult.empty();
        }
        try {
            Calendar client = buildClient();
            AtomicInteger created = new AtomicInteger();
            AtomicInteger updated = new AtomicInteger();
            AtomicInteger deleted = new AtomicInteger();
            AtomicInteger failed  = new AtomicInteger();

            record Op(String label, BatchRequestAdder adder) {}
            List<Op> ops = new ArrayList<>(toCreate.size() + toUpdate.size() + toDeleteIds.size());

            for (OutlookEvent oe : toCreate) {
                Event event = toGoogleApiEvent(oe, syncColorLabels);
                ops.add(new Op("CREATE " + oe.subject(), batch ->
                        batch.queue(
                                client.events().insert(calendarId, event).buildHttpRequest(),
                                Event.class, com.google.api.client.googleapis.json.GoogleJsonErrorContainer.class,
                                new BatchCallback<>() {
                                    public void onSuccess(Event e, com.google.api.client.http.HttpHeaders h) { created.incrementAndGet(); }
                                    public void onFailure(com.google.api.client.googleapis.json.GoogleJsonErrorContainer e, com.google.api.client.http.HttpHeaders h) {
                                        log.warn("CREATE failed for '{}': {}", oe.subject(), e.getError().getMessage());
                                        failed.incrementAndGet();
                                    }
                                })));
            }

            for (Map.Entry<String, OutlookEvent> entry : toUpdate.entrySet()) {
                String googleId = entry.getKey();
                OutlookEvent oe = entry.getValue();
                Event event = toGoogleApiEvent(oe, syncColorLabels);
                ops.add(new Op("UPDATE " + oe.subject(), batch ->
                        batch.queue(
                                client.events().update(calendarId, googleId, event).buildHttpRequest(),
                                Event.class, com.google.api.client.googleapis.json.GoogleJsonErrorContainer.class,
                                new BatchCallback<>() {
                                    public void onSuccess(Event e, com.google.api.client.http.HttpHeaders h) { updated.incrementAndGet(); }
                                    public void onFailure(com.google.api.client.googleapis.json.GoogleJsonErrorContainer e, com.google.api.client.http.HttpHeaders h) {
                                        log.warn("UPDATE failed for '{}': {}", oe.subject(), e.getError().getMessage());
                                        failed.incrementAndGet();
                                    }
                                })));
            }

            for (String googleId : toDeleteIds) {
                ops.add(new Op("DELETE " + googleId, batch ->
                        batch.queue(
                                client.events().delete(calendarId, googleId).buildHttpRequest(),
                                Void.class, com.google.api.client.googleapis.json.GoogleJsonErrorContainer.class,
                                new BatchCallback<>() {
                                    public void onSuccess(Void v, com.google.api.client.http.HttpHeaders h) { deleted.incrementAndGet(); }
                                    public void onFailure(com.google.api.client.googleapis.json.GoogleJsonErrorContainer e, com.google.api.client.http.HttpHeaders h) {
                                        log.warn("DELETE failed for '{}': {}", googleId, e.getError().getMessage());
                                        failed.incrementAndGet();
                                    }
                                })));
            }

            // Execute in chunks of 50 (Google API limit per batch)
            int chunkSize = 50;
            for (int i = 0; i < ops.size(); i += chunkSize) {
                List<Op> chunk = ops.subList(i, Math.min(i + chunkSize, ops.size()));
                BatchRequest batch = client.batch();
                for (Op op : chunk) {
                    op.adder().addTo(batch);
                }
                log.debug("Executing batch of {} operations ({}/{})", chunk.size(), i + chunk.size(), ops.size());
                batch.execute();
            }

            return new BatchWriteResult(created.get(), updated.get(), deleted.get(), failed.get());

        } catch (Exception e) {
            log.error("Batch write failed", e);
            throw new RuntimeException("Batch write failed: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface BatchRequestAdder {
        void addTo(BatchRequest batch) throws IOException;
    }

    /**
     * Resolves the stored principal name from DB and loads the authorized client.
     * This means the adapter works without an active HTTP session — survives restarts
     * as long as the refresh token is in the JDBC store.
     */
    private Calendar buildClient() throws GeneralSecurityException, IOException {
        String principalName = profileService.getOrCreate().getGooglePrincipalName();
        if (principalName == null) {
            throw new IllegalStateException("Google not connected: no principal stored in profile");
        }
        OAuth2AuthorizedClient ac = authorizedClientService.loadAuthorizedClient("google", principalName);
        if (ac == null || ac.getAccessToken() == null) {
            throw new IllegalStateException("No authorized Google client in JDBC store for: " + principalName);
        }

        String token = ac.getAccessToken().getTokenValue();
        Instant expiresAt = ac.getAccessToken().getExpiresAt();
        String refresh = ac.getRefreshToken() != null ? ac.getRefreshToken().getTokenValue() : null;
        Date expDate = expiresAt != null ? Date.from(expiresAt) : new Date(System.currentTimeMillis() + 3600_000L);

        GoogleCredentials creds = refresh != null
                ? UserCredentials.newBuilder()
                        .setClientId(clientId).setClientSecret(clientSecret)
                        .setAccessToken(new AccessToken(token, expDate))
                        .setRefreshToken(refresh).build()
                : GoogleCredentials.create(new AccessToken(token, expDate));

        return new Calendar.Builder(trustAllTransport(), GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(creds))
                .setApplicationName(applicationName)
                .build();
    }

    private NetHttpTransport trustAllTransport() {
        try {
            return new NetHttpTransport.Builder().doNotValidateCertificate().build();
        } catch (GeneralSecurityException e) {
            log.warn("Trust-all transport failed, falling back to default: {}", e.getMessage());
            try { return GoogleNetHttpTransport.newTrustedTransport(); }
            catch (GeneralSecurityException | IOException ex) {
                throw new IllegalStateException("Failed to build HTTP transport", ex);
            }
        }
    }

    private Event toGoogleApiEvent(OutlookEvent src, boolean syncColorLabels) {
        Event event = new Event();
        event.setSummary(src.subject());
        event.setDescription(src.body());
        event.setLocation(src.location());

        if (syncColorLabels && src.colorIndex() > 0) {
            String googleColorId = OUTLOOK_TO_GOOGLE_COLOR.get(src.colorIndex());
            if (googleColorId != null) event.setColorId(googleColorId);
        }

        Map<String, String> priv = new HashMap<>();
        priv.put(SYNC_SOURCE_KEY, SYNC_SOURCE_VALUE);
        priv.put(OUTLOOK_ID_KEY, src.id());
        event.setExtendedProperties(new Event.ExtendedProperties().setPrivate(priv));

        ZoneId zone = src.start() != null ? src.start().getZone() : ZoneId.of("UTC");
        if (src.allDay()) {
            event.setStart(new EventDateTime().setDate(new DateTime(src.start().toLocalDate().toString())));
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
