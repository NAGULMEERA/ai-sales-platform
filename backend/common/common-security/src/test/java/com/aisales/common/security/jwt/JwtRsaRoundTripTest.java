package com.aisales.common.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.security.util.JwtTokenProvider;
import com.aisales.common.security.util.JwtUtils;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class JwtRsaRoundTripTest {

    private PlatformRsaKeyProvider keyProvider;
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

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        keyProvider = new PlatformRsaKeyProvider(properties, env);
        ReflectionTestUtils.invokeMethod(keyProvider, "init");

        tokenProvider = new JwtTokenProvider(keyProvider, properties);
        jwtUtils = new JwtUtils(keyProvider);
    }

    @Test
    void shouldSignAndVerifyAccessTokenWithRsa() {
        var tokens = tokenProvider.generateAccessToken(
                "user-1", "tenant-1", null, "a@b.com", Set.of("TENANT_ADMIN"), Set.of("lead:read"), "PREMIUM");

        var claims = jwtUtils.parseClaims(tokens.getAccessToken());
        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("tenantId", String.class)).isEqualTo("tenant-1");
        assertThat(claims.get("subscriptionPlan", String.class)).isEqualTo("PREMIUM");
        assertThat(claims.getIssuer()).isEqualTo("aisales-platform");
        assertThat(claims.getAudience()).contains("aisales-api");
        assertThat(keyProvider.jwks().toString()).contains("aisales-1");
    }
}
