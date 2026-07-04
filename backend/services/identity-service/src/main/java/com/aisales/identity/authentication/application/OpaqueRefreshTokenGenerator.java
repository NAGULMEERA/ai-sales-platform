package com.aisales.identity.authentication.application;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;



/**
 * Opaque refresh tokens stored in the database (not JWTs).
 * Keeps refresh payloads small and avoids duplicating permission claims.
 */
@Component
public class OpaqueRefreshTokenGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
