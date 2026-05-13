package pl.qprogramming.calendarsync.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import pl.qprogramming.calendarsync.service.ProfileService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig")
class SecurityConfigTest {

    @Mock private OAuth2AuthorizedClientService authorizedClientService;
    @Mock private ClientRegistrationRepository clientRegistrationRepository;
    @Mock private ProfileService profileService;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("tokenResponseClient() returns a configured token response client")
    void tokenResponseClient_returnsConfiguredClient() {
        DefaultAuthorizationCodeTokenResponseClient client = securityConfig.tokenResponseClient();
        assertThat(client).isNotNull();
    }
}
