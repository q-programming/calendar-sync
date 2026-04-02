package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.repository.ProfileRepository;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    public ProfileEntity getOrCreate() {
        return profileRepository.findById(1L).orElseGet(() -> profileRepository.save(new ProfileEntity()));
    }

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

    public void setOutlookCalendar(String calendarId, String calendarName) {
        ProfileEntity profile = getOrCreate();
        profile.setOutlookCalendarId(calendarId);
        profile.setOutlookCalendarName(calendarName);
        profileRepository.save(profile);
    }

    public void setGoogleCalendar(String calendarId, String calendarName) {
        ProfileEntity profile = getOrCreate();
        profile.setGoogleCalendarId(calendarId);
        profile.setGoogleCalendarName(calendarName);
        profileRepository.save(profile);
    }

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
