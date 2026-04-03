package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.repository.ProfileRepository;

import java.io.File;

/**
 * Manages the user's connection profile stored as a single database row ({@code id = 1}).
 *
 * <p>A profile encapsulates all identity and calendar-selection data for both the Outlook
 * (PST/OST file path) and Google (OAuth2 principal + selected calendar) sides of the sync.
 * A default empty row is created on first access so callers never receive {@code null}.
 *
 * <p>Google connection state is derived from the JDBC OAuth token store, not from an active
 * HTTP session, so {@link #isGoogleConnected()} works reliably across application restarts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    /**
     * Returns the current profile row, creating a default empty one if none exists yet.
     *
     * @return the singleton {@link ProfileEntity}; never {@code null}
     */
    public ProfileEntity getOrCreate() {
        return profileRepository.findById(1L).orElseGet(() -> profileRepository.save(new ProfileEntity()));
    }

    /**
     * Validates the given file path and, if it resolves to a readable file, stores it as
     * the Outlook PST/OST profile path.
     *
     * @param profilePath absolute file-system path to the Outlook data file
     * @throws IllegalArgumentException if the path is blank, does not exist, or is not readable
     */
    public void connectOutlook(String profilePath) {
        File file = new File(profilePath);
        if (!file.exists() || !file.canRead()) {
            throw new IllegalArgumentException(
                "Outlook profile path does not exist or is not readable: " + profilePath);
        }
        ProfileEntity profile = getOrCreate();
        profile.setOutlookProfilePath(profilePath);
        profileRepository.save(profile);
    }

    /**
     * Stores the user's selected Outlook calendar id and display name.
     * Both values come from the list returned by
     * {@link pl.qprogramming.calendarsync.service.outlook.OutlookCalendarService#listCalendars}.
     *
     * @param calendarId   internal folder descriptor id used to locate the calendar in the PST
     * @param calendarName human-readable display name shown in the UI
     */
    public void setOutlookCalendar(String calendarId, String calendarName) {
        ProfileEntity profile = getOrCreate();
        profile.setOutlookCalendarId(calendarId);
        profile.setOutlookCalendarName(calendarName);
        profileRepository.save(profile);
    }

    /**
     * Stores the user's selected Google calendar id and display name.
     * Both values come from the list returned by
     * {@link pl.qprogramming.calendarsync.service.google.GoogleCalendarService#listCalendars}.
     *
     * @param calendarId   Google Calendar API id (e.g. {@code primary} or a full email-based id)
     * @param calendarName human-readable display name shown in the UI
     */
    public void setGoogleCalendar(String calendarId, String calendarName) {
        ProfileEntity profile = getOrCreate();
        profile.setGoogleCalendarId(calendarId);
        profile.setGoogleCalendarName(calendarName);
        profileRepository.save(profile);
    }

    /**
     * Disconnects the Google account by removing the stored OAuth2 token and clearing
     * all Google-related profile fields.
     *
     * <p>The token is removed from the JDBC {@link OAuth2AuthorizedClientService} store so
     * subsequent API calls fail fast with a clear error rather than using a stale token.
     * Removal failures are logged as warnings and do not block the profile update.
     */
    public void disconnectGoogle() {
        ProfileEntity profile = getOrCreate();
        String principalName = profile.getGooglePrincipalName();
        if (principalName != null) {
            try {
                authorizedClientService.removeAuthorizedClient("google", principalName);
                log.info("Removed JDBC token for Google principal: {}", principalName);
            } catch (Exception e) {
                log.warn("Could not remove JDBC token for {}: {}", principalName, e.getMessage());
            }
        }
        profile.setGooglePrincipalName(null);
        profile.setGoogleCalendarId(null);
        profile.setGoogleCalendarName(null);
        profileRepository.save(profile);
    }

    /**
     * Clears all Outlook-related profile fields (path, calendar id, and display name).
     * After this call the profile shows Outlook as disconnected.
     */
    public void disconnectOutlook() {
        ProfileEntity profile = getOrCreate();
        profile.setOutlookProfilePath(null);
        profile.setOutlookCalendarId(null);
        profile.setOutlookCalendarName(null);
        profileRepository.save(profile);
    }

    /**
     * Called after successful OAuth2 login to persist the principal name.
     * This is the source of truth for identifying the JDBC-stored token across restarts.
     */
    public void saveGooglePrincipal(String principalName) {
        ProfileEntity profile = getOrCreate();
        if (!principalName.equals(profile.getGooglePrincipalName())) {
            log.info("Saving Google principal: {}", principalName);
            profile.setGooglePrincipalName(principalName);
            profileRepository.save(profile);
        }
    }

    /**
     * Checks Google connection by looking up the stored principal name in the JDBC token store.
     * Does NOT require an active HTTP session — works after application restarts.
     */
    public boolean isGoogleConnected() {
        String principalName = getOrCreate().getGooglePrincipalName();
        if (principalName == null) return false;
        try {
            return authorizedClientService.loadAuthorizedClient("google", principalName) != null;
        } catch (Exception e) {
            log.warn("Error checking Google connection for {}: {}", principalName, e.getMessage());
            return false;
        }
    }
}
