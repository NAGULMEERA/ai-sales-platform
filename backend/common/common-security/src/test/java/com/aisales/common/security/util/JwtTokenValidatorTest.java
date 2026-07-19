package com.aisales.common.security.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.security.jwt.JwtRsaProperties;
import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import com.aisales.common.security.model.UserPrincipal;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private JwtTokenProvider tokenProvider;
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        JwtRsaProperties properties = new JwtRsaProperties();
        properties.setSigningEnabled(true);
        properties.setPrivateKeyLocation("classpath:jwt/local-private.pem");
        properties.setPublicKeyLocation("classpath:jwt/local-public.pem");
        properties.setKeyId("aisales-1");
        properties.setIssuer("aisales-platform");
        properties.setAudience("aisales-api");
        properties.setAccessTokenExpirationMs(3_600_000L);
        properties.setRefreshTokenExpirationMs(86_400_000L);

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        PlatformRsaKeyProvider keyProvider = new PlatformRsaKeyProvider(properties, env);
        ReflectionTestUtils.invokeMethod(keyProvider, "init");

        tokenProvider = new JwtTokenProvider(keyProvider, properties);
        jwtUtils = new JwtUtils(keyProvider);
        validator = new JwtTokenValidator(jwtUtils, properties);
    }

    @Test
    void shouldAcceptAccessTokenAndRejectRefreshToken() {
        var tokens = tokenProvider.generateTokens(
                "user-1", "tenant-1", "org-1", "a@b.com", Set.of("AGENT"), Set.of("lead:read"));

        assertThatCode(() -> validator.validateAccessToken(tokens.getAccessToken()))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validateAccessToken(tokens.getRefreshToken()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("token type");
    }

    @Test
    void shouldMapPermissionsIntoAuthorities() {
        var tokens = tokenProvider.generateTokens(
                "user-1", "tenant-1", "org-1", "a@b.com", Set.of("USER"), Set.of("lead:read", "ai:execute"));
        var claims = validator.validateAccessToken(tokens.getAccessToken());
        UserPrincipal principal = jwtUtils.toUserPrincipal(claims);

        assertThat(principal.getPermissions()).containsExactlyInAnyOrder("lead:read", "ai:execute");
        assertThat(principal.getAuthorities())
                .extracting(a -> a.getAuthority())
                .contains("ROLE_USER", "lead:read", "ai:execute");
        assertThat(claims.getIssuer()).isEqualTo("aisales-platform");
        assertThat(claims.getAudience()).contains("aisales-api");
    }
}
