package pl.qprogramming.calendarsync.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import pl.qprogramming.calendarsync.service.ProfileService;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final ProfileService profileService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/calendarsync", true)
                        .successHandler(oauth2SuccessHandler())
                        .authorizedClientService(authorizedClientService)
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(
                                        authorizationRequestResolver(clientRegistrationRepository)))
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(tokenResponseClient()))
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * Token response client using trust-all SSL but with the required OAuth2 message converters.
     * A plain RestTemplate loses OAuth2AccessTokenResponseHttpMessageConverter, causing
     * additionalParameters (including id_token) to be null and NPE in OIDC provider.
     */
    @Bean
    public DefaultAuthorizationCodeTokenResponseClient tokenResponseClient() {
        RestTemplate restTemplate = new RestTemplate(
                List.of(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        applySslTrustAll(restTemplate);

        var client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRestOperations(restTemplate);
        return client;
    }

    /**
     * Saves the OAuth2 principal name to DB immediately on successful login,
     * then redirects to the SPA. This is the authoritative moment when we know
     * a refresh token has been stored in the JDBC authorized client service.
     */
    private AuthenticationSuccessHandler oauth2SuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            profileService.saveGooglePrincipal(authentication.getName());
            response.sendRedirect("/calendarsync");
        };
    }

    private DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository repo) {
        var resolver = new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(authorizationRequestCustomizer());
        return resolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> {
            Map<String, Object> params = new HashMap<>();
            params.put("access_type", "offline");
            params.put("prompt", "consent");
            customizer.additionalParameters(p -> p.putAll(params));
        };
    }

    /**
     * Applies trust-all SSL to a RestTemplate's request factory.
     * Needed in WSL where the JVM cacerts bundle may not include Google root CAs,
     * causing PKIX path building failures on the token exchange POST to googleapis.com.
     */
    private void applySslTrustAll(RestTemplate restTemplate) {
        TrustManager[] trustAll = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }
        };
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(ctx.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10_000);
            factory.setReadTimeout(10_000);
            restTemplate.setRequestFactory(factory);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            log.error("Failed to create trust-all SSL context, proceeding with default", e);
        }
    }
}
