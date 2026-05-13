package pl.qprogramming.calendarsync.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthController")
class AuthControllerTest {

    private final AuthController controller = new AuthController();

    @Test
    @DisplayName("login() redirects to Google OAuth2 authorization endpoint")
    void login_redirectsToGoogle() {
        String result = controller.login();
        assertThat(result).isEqualTo("redirect:/oauth2/authorization/google");
    }
}
