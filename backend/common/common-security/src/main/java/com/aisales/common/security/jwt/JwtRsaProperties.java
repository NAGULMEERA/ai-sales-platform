package com.aisales.common.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.security.jwt")
public class JwtRsaProperties {

    /**
     * When true, this service may mint access tokens (identity-service only).
     */
    private boolean signingEnabled = false;

    /**
     * PKCS#8 PEM content or Spring resource location for the signing private key.
     */
    private String privateKeyLocation;

    /**
     * X.509 PEM content or Spring resource location for the verification public key.
     * When empty, falls back to {@link #jwkSetUri} or the local/dev classpath public key.
     */
    private String publicKeyLocation;

    /**
     * Optional JWKS URI (e.g. http://identity-service:8081/.well-known/jwks.json).
     * Used when {@link #publicKeyLocation} is not set, and for {@code JwtDecoder}.
     */
    private String jwkSetUri;

    private String keyId = "aisales-1";

    private long accessTokenExpirationMs = 3_600_000L;

    private long refreshTokenExpirationMs = 86_400_000L;
}
