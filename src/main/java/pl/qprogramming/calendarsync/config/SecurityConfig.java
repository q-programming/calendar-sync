package pl.qprogramming.calendarsync.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/"),
                                new AntPathRequestMatcher("/index.html"),
                                new AntPathRequestMatcher("/static/**"),
                                new AntPathRequestMatcher("/assets/**"),
                                new AntPathRequestMatcher("/*.js"),
                                new AntPathRequestMatcher("/*.css"),
                                new AntPathRequestMatcher("/*.json"),
                                new AntPathRequestMatcher("/*.ico"),
                                new AntPathRequestMatcher("/*.png"),
                                new AntPathRequestMatcher("/favicon.svg"),
                                new AntPathRequestMatcher("/manifest.json"),
                                new AntPathRequestMatcher("/error")
                        ).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/weather/**"), new AntPathRequestMatcher("/weather/**")).permitAll()
                        .requestMatchers(
                                new AntPathRequestMatcher("/api/auth/login"),
                                new AntPathRequestMatcher("/api/auth/user"),
                                new AntPathRequestMatcher("/api/auth/token"),
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/auth/user"),
                                new AntPathRequestMatcher("/auth/token")
                        ).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/calendar/**"), new AntPathRequestMatcher("/calendar/**")).authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/api/auth/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/api/auth/login?error=true")
                        .authorizedClientService(authorizedClientService)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository))
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessUrl("/")
                        .clearAuthentication(true)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new Http403ForbiddenEntryPoint()))
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Custom OAuth2AuthorizationRequestResolver to add additional parameters
     * to the authorization request, such as access_type and prompt.
     * This is necessary to ensure that the refresh token is always provided
     *
     * @param clientRegistrationRepository client registration repository
     * @return OAuth2AuthorizationRequestResolver
     */
    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        authorizationRequestResolver.setAuthorizationRequestCustomizer(
                authorizationRequestCustomizer());
        return authorizationRequestResolver;
    }

    /**
     * Customizer for OAuth2AuthorizationRequest to add additional parameters
     *
     * @return Consumer<OAuth2AuthorizationRequest.Builder>
     */
    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> {
            Map<String, Object> additionalParams = new HashMap<>();
            additionalParams.put("access_type", "offline");
            additionalParams.put("prompt", "consent");  // Always ask for consent to ensure we get a refresh token
            customizer.additionalParameters(params -> params.putAll(additionalParams));
        };
    }
}
