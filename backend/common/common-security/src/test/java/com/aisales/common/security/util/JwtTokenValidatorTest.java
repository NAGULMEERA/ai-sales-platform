package com.aisales.common.security.util;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.security.jwt.JwtRsaProperties;
import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtRsaProperties properties = new JwtRsaProperties();
        properties.setSigningEnabled(true);
        properties.setPrivateKeyLocation("classpath:jwt/local-private.pem");
        properties.setPublicKeyLocation("classpath:jwt/local-public.pem");
        properties.setKeyId("aisales-1");
        properties.setAccessTokenExpirationMs(3_600_000L);
        properties.setRefreshTokenExpirationMs(86_400_000L);

        PlatformRsaKeyProvider keyProvider = new PlatformRsaKeyProvider(properties);
        ReflectionTestUtils.invokeMethod(keyProvider, "init");

        tokenProvider = new JwtTokenProvider(keyProvider, properties);
        validator = new JwtTokenValidator(new JwtUtils(keyProvider));
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
}
