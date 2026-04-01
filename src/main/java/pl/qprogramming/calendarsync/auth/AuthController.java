package pl.qprogramming.calendarsync.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import pl.qprogramming.calendarsync.api.AuthApiDelegate;

@Slf4j
@Component
public class AuthController implements AuthApiDelegate {

    @GetMapping("/api/auth/login")
    public String login() {
        log.info("Redirecting to OAuth2 login page");
        return "redirect:/oauth2/authorization/google";
    }

    @Override
    public ResponseEntity<Void> authLogin() {
        // Handled by @GetMapping above; this satisfies the delegate contract
        return ResponseEntity.status(302).build();
    }
}
