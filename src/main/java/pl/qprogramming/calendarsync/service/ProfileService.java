package pl.qprogramming.calendarsync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.stereotype.Service;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.repository.ProfileRepository;

import java.io.File;
import java.security.Principal;

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

    public boolean isGoogleConnected(Principal principal) {
        if (principal == null) return false;
        try {
            boolean connected = authorizedClientService.loadAuthorizedClient("google", principal.getName()) != null;
            if (connected) {
                // Persist principal name for background scheduler use
                ProfileEntity profile = getOrCreate();
                if (!principal.getName().equals(profile.getGooglePrincipalName())) {
                    profile.setGooglePrincipalName(principal.getName());
                    profileRepository.save(profile);
                }
            }
            return connected;
        } catch (Exception e) {
            return false;
        }
    }
}
