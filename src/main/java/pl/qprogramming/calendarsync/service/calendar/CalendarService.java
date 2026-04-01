package pl.qprogramming.calendarsync.service.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static pl.qprogramming.calendarsync.config.CacheConfig.CacheNames;

/**
 * Service for interacting with Google Calendar API.
 * <p>
 * This service provides methods to access Google Calendar data using OAuth2 authentication.
 * It supports retrieving calendar lists and calendar events for authenticated users.
 * Results are cached for 5 minutes to reduce API calls and improve performance.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final CalendarMapper calendarMapper;

    /**
     * Creates a Google Calendar client with the provided access token.
     * <p>
     * This method initializes a Google Calendar API client with the necessary authentication
     * and configuration to make API requests on behalf of the user.
     * </p>
     *
     * @param accessToken OAuth2 access token for Google Calendar API
     * @param expiresAt   Token's actual expiration time
     * @return Configured Calendar service instance
     * @throws GeneralSecurityException If there's a security-related error when creating the transport
     * @throws IOException              If there's an I/O error during client creation
     */
    Calendar createCalendarClient(String accessToken, Instant expiresAt, String refreshToken) throws GeneralSecurityException, IOException {
        // If no expiration time is provided, use a default (1 hour from now)
        Date expirationTime = expiresAt != null
                ? Date.from(expiresAt)
                : new Date(System.currentTimeMillis() + 3600 * 1000); // 1 hour from now

        // Create credentials with both access and refresh tokens if available
        GoogleCredentials credentials;
        if (refreshToken != null) {
            // Use UserCredentials when we have a refresh token
            credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setAccessToken(new AccessToken(accessToken, expirationTime))
                    .setRefreshToken(refreshToken)
                    .build();
            log.debug("Created UserCredentials with refresh token capabilities");
        } else {
            // Fallback to simple credentials without refresh capabilities
            credentials = GoogleCredentials.create(new AccessToken(accessToken, expirationTime));
            log.debug("Created simple GoogleCredentials without refresh capabilities");
        }

        val requestInitializer = new HttpCredentialsAdapter(credentials);
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName(applicationName)
                .build();
    }

    /**
     * Retrieves the list of calendars available to the authenticated user.
     * <p>
     * This method returns all calendar entries that the user has access to, including
     * primary calendar, secondary calendars, and shared calendars.
     * Results are cached for 5 minutes to reduce API calls.
     * </p>
     *
     * @param accessToken  OAuth2 access token for Google Calendar API
     * @param expiresAt    Token's expiration time
     * @param refreshToken OAuth2 refresh token (optional)
     * @return List of calendar entries accessible to the user
     * @throws GeneralSecurityException If there's a security-related error
     * @throws IOException              If there's an I/O error during the API call
     */
    @Cacheable(value = CacheNames.CALENDAR_LIST, key = "#accessToken", cacheManager = "calendarCacheManager")
    public List<CalendarListEntry> getCalendarList(String accessToken, Instant expiresAt, String refreshToken) throws GeneralSecurityException, IOException {
        log.debug("Fetching calendar list for access token: {}", accessToken);
        val calendarClient = createCalendarClient(accessToken, expiresAt, refreshToken);
        val calendarList = calendarClient.calendarList().list().execute();
        return calendarList.getItems();
    }
}
