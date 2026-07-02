package com.aisales.identity.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenValidator;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies sensible connect/read timeouts to every outbound HTTP call made by the Google OAuth2
 * login flow: the authorization-code -&gt; access-token exchange, the UserInfo endpoint call, and
 * the JWK Set fetch used to validate the ID Token signature. Spring Security's defaults for all
 * three use a plain {@code new RestTemplate()} with no timeout configured, which can hang
 * indefinitely if Google's endpoints are slow or unreachable (Rule 08/09 resilience: timeouts
 * are the highest-priority resilience control).
 */
@Configuration
public class OAuth2ClientResilienceConfiguration {

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient(
            @Value("${aisales.oauth2.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${aisales.oauth2.read-timeout-ms:5000}") long readTimeoutMs) {
        // Must keep the same converters/error handler as Spring Security's default
        // DefaultAuthorizationCodeTokenResponseClient() constructor; only the request factory
        // (connect/read timeout) is being customized.
        RestTemplate restTemplate = new RestTemplate(
                Arrays.asList(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        restTemplate.setRequestFactory(timeoutRequestFactory(connectTimeoutMs, readTimeoutMs));

        DefaultAuthorizationCodeTokenResponseClient client = new DefaultAuthorizationCodeTokenResponseClient();
        client.setRestOperations(restTemplate);
        return client;
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService(
            @Value("${aisales.oauth2.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${aisales.oauth2.read-timeout-ms:5000}") long readTimeoutMs) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(timeoutRequestFactory(connectTimeoutMs, readTimeoutMs));

        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        delegate.setRestOperations(restTemplate);

        OidcUserService oidcUserService = new OidcUserService();
        oidcUserService.setOauth2UserService(delegate);
        return oidcUserService;
    }

    /**
     * Auto-detected by Spring Security's {@code OAuth2LoginConfigurer} (looks up a bean of type
     * {@code JwtDecoderFactory<ClientRegistration>}); no explicit wiring in the security filter
     * chain is required. Reproduces the same ID Token validation that the default
     * {@code OidcIdTokenDecoderFactory} applies, since {@code OidcIdTokenDecoderFactory} itself
     * does not expose a way to configure the JWK Set fetch timeout (fixed upstream in Spring
     * Security 6.3+, not yet available on 6.2.x).
     *
     * <p><b>Verified against Spring Security 6.2.4 source</b>
     * ({@code OidcIdTokenDecoderFactory}): the default factory (1) resolves the JWS algorithm to
     * {@link SignatureAlgorithm#RS256} unless a client explicitly configures a different one (all
     * our registrations, including Google, use RS256), and (2) composes exactly
     * {@code new JwtTimestampValidator()} + {@code new OidcIdTokenValidator(clientRegistration)}
     * via {@code DefaultOidcIdTokenValidatorFactory} &mdash; i.e. expiry/issued-at (timestamp),
     * issuer, audience, and (when present) nonce/azp checks. Both are reproduced identically
     * below; only the JWK Set fetch's {@link RestTemplate} gains a connect/read timeout. No
     * validation is bypassed, weakened, or reordered.
     */
    @Bean
    public JwtDecoderFactory<ClientRegistration> idTokenDecoderFactory(
            @Value("${aisales.oauth2.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${aisales.oauth2.read-timeout-ms:5000}") long readTimeoutMs) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(timeoutRequestFactory(connectTimeoutMs, readTimeoutMs));

        Map<String, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();
        return clientRegistration -> jwtDecoders.computeIfAbsent(clientRegistration.getRegistrationId(), key -> {
            NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                    .withJwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri())
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .restOperations(restTemplate)
                    .build();
            OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                    new JwtTimestampValidator(), new OidcIdTokenValidator(clientRegistration));
            jwtDecoder.setJwtValidator(validator);
            return jwtDecoder;
        });
    }

    private static ClientHttpRequestFactory timeoutRequestFactory(long connectTimeoutMs, long readTimeoutMs) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .withReadTimeout(Duration.ofMillis(readTimeoutMs));
        return ClientHttpRequestFactories.get(settings);
    }
}
