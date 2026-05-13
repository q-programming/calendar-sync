package pl.qprogramming.calendarsync.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import pl.qprogramming.calendarsync.entity.ProfileEntity;
import pl.qprogramming.calendarsync.repository.ProfileRepository;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService")
class ProfileServiceTest {

    @Mock
    ProfileRepository profileRepository;

    @Mock
    OAuth2AuthorizedClientService authorizedClientService;

    @InjectMocks
    ProfileService profileService;

    private ProfileEntity savedProfile(String outlookPath) {
        var p = new ProfileEntity();
        p.setOutlookProfilePath(outlookPath);
        return p;
    }

    // ── getOrCreate ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreate")
    class GetOrCreate {

        @Test
        @DisplayName("returns existing row when present")
        void returnsExisting_WhenPresent() {
            var existing = new ProfileEntity();
            when(profileRepository.findById(1L)).thenReturn(Optional.of(existing));

            var result = profileService.getOrCreate();

            assertThat(result).isSameAs(existing);
            verify(profileRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates and saves new profile when not found")
        void createsNew_WhenNotFound() {
            var saved = new ProfileEntity();
            when(profileRepository.findById(1L)).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenReturn(saved);

            var result = profileService.getOrCreate();

            assertThat(result).isEqualTo(saved);
            verify(profileRepository).save(any(ProfileEntity.class));
        }
    }

    // ── connectOutlook ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("connectOutlook")
    class ConnectOutlook {

        @Test
        @DisplayName("saves path when file exists and is readable")
        void savesPath_WhenFileReadable(@TempDir Path tempDir) throws Exception {
            File file = tempDir.resolve("outlook.pst").toFile();
            file.createNewFile();

            var profile = new ProfileEntity();
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            profileService.connectOutlook(file.getAbsolutePath());

            ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getOutlookProfilePath()).isEqualTo(file.getAbsolutePath());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when file does not exist")
        void throwsIAE_WhenFileNotExist() {
            assertThatThrownBy(() -> profileService.connectOutlook("/nonexistent/path.pst"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not readable");
        }
    }

    // ── setOutlookCalendar ────────────────────────────────────────────────────

    @Nested
    @DisplayName("setOutlookCalendar")
    class SetOutlookCalendar {

        @Test
        @DisplayName("saves calendarId and calendarName")
        void savesFields() {
            var profile = new ProfileEntity();
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            profileService.setOutlookCalendar("cal-id", "My Calendar");

            ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getOutlookCalendarId()).isEqualTo("cal-id");
            assertThat(captor.getValue().getOutlookCalendarName()).isEqualTo("My Calendar");
        }
    }

    // ── setGoogleCalendar ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("setGoogleCalendar")
    class SetGoogleCalendar {

        @Test
        @DisplayName("saves googleCalendarId and googleCalendarName")
        void savesFields() {
            var profile = new ProfileEntity();
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            profileService.setGoogleCalendar("gcal-id", "Google Cal");

            ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getGoogleCalendarId()).isEqualTo("gcal-id");
            assertThat(captor.getValue().getGoogleCalendarName()).isEqualTo("Google Cal");
        }
    }

    // ── disconnectGoogle ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("disconnectGoogle")
    class DisconnectGoogle {

        @Test
        @DisplayName("removes token and clears Google fields")
        void removesTokenAndClearsFields() {
            var profile = new ProfileEntity();
            profile.setGooglePrincipalName("user@example.com");
            profile.setGoogleCalendarId("gcal");
            profile.setGoogleCalendarName("Cal");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            profileService.disconnectGoogle();

            verify(authorizedClientService).removeAuthorizedClient("google", "user@example.com");
            ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getGooglePrincipalName()).isNull();
            assertThat(captor.getValue().getGoogleCalendarId()).isNull();
            assertThat(captor.getValue().getGoogleCalendarName()).isNull();
        }

        @Test
        @DisplayName("skips token removal when principalName is null")
        void skipsTokenRemoval_WhenNoPrincipal() {
            var profile = new ProfileEntity(); // no principal
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            profileService.disconnectGoogle();

            verifyNoInteractions(authorizedClientService);
            verify(profileRepository).save(any());
        }

        @Test
        @DisplayName("swallows token removal exception and still clears fields")
        void swallowsTokenRemovalException() {
            var profile = new ProfileEntity();
            profile.setGooglePrincipalName("user@example.com");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doThrow(new RuntimeException("token store error"))
                    .when(authorizedClientService).removeAuthorizedClient(any(), any());

            profileService.disconnectGoogle();

            // Should not throw, and still save profile
            verify(profileRepository).save(any());
        }
    }

    // ── disconnectOutlook ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("disconnectOutlook")
    class DisconnectOutlook {

        @Test
        @DisplayName("clears all Outlook fields")
        void clearsOutlookFields() {
            var profile = new ProfileEntity();
            profile.setOutlookProfilePath("/some/path.pst");
            profile.setOutlookCalendarId("cal-id");
            profile.setOutlookCalendarName("Cal");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            profileService.disconnectOutlook();

            ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getOutlookProfilePath()).isNull();
            assertThat(captor.getValue().getOutlookCalendarId()).isNull();
            assertThat(captor.getValue().getOutlookCalendarName()).isNull();
        }
    }

    // ── saveGooglePrincipal ───────────────────────────────────────────────────

    @Nested
    @DisplayName("saveGooglePrincipal")
    class SaveGooglePrincipal {

        @Test
        @DisplayName("saves principal when it is new")
        void savesPrincipal_WhenNew() {
            var profile = new ProfileEntity(); // no principal
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            profileService.saveGooglePrincipal("newuser@example.com");

            ArgumentCaptor<ProfileEntity> captor = ArgumentCaptor.forClass(ProfileEntity.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getGooglePrincipalName()).isEqualTo("newuser@example.com");
        }

        @Test
        @DisplayName("skips save when principal is unchanged")
        void skipsSave_WhenUnchanged() {
            var profile = new ProfileEntity();
            profile.setGooglePrincipalName("same@example.com");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

            profileService.saveGooglePrincipal("same@example.com");

            verify(profileRepository, never()).save(any());
        }
    }

    // ── isGoogleConnected ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("isGoogleConnected")
    class IsGoogleConnected {

        @Test
        @DisplayName("returns false when principalName is null")
        void returnsFalse_WhenNoPrincipal() {
            var profile = new ProfileEntity();
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

            assertThat(profileService.isGoogleConnected()).isFalse();
        }

        @Test
        @DisplayName("returns true when token found in store")
        void returnsTrue_WhenTokenFound() {
            var profile = new ProfileEntity();
            profile.setGooglePrincipalName("user@example.com");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(authorizedClientService.loadAuthorizedClient("google", "user@example.com"))
                    .thenReturn(mock(OAuth2AuthorizedClient.class));

            assertThat(profileService.isGoogleConnected()).isTrue();
        }

        @Test
        @DisplayName("returns false when token is null")
        void returnsFalse_WhenTokenNull() {
            var profile = new ProfileEntity();
            profile.setGooglePrincipalName("user@example.com");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(authorizedClientService.loadAuthorizedClient("google", "user@example.com"))
                    .thenReturn(null);

            assertThat(profileService.isGoogleConnected()).isFalse();
        }

        @Test
        @DisplayName("returns false when token store throws exception")
        void returnsFalse_WhenExceptionThrown() {
            var profile = new ProfileEntity();
            profile.setGooglePrincipalName("user@example.com");
            when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));
            when(authorizedClientService.loadAuthorizedClient(any(), any()))
                    .thenThrow(new RuntimeException("store error"));

            assertThat(profileService.isGoogleConnected()).isFalse();
        }
    }
}
