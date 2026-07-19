package com.aisales.common.security.jwt;

import com.aisales.common.security.util.JwtTokenProvider;
import com.aisales.common.security.util.JwtUtils;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

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
        properties.setAccessTokenExpirationMs(3_600_000L);

        keyProvider = new PlatformRsaKeyProvider(properties);
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
        assertThat(keyProvider.jwks().toString()).contains("aisales-1");
    }
}
