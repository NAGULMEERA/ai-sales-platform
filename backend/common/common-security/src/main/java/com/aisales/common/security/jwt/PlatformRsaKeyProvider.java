package com.aisales.common.security.jwt;

import com.aisales.common.core.security.PemKeyLoader;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Platform RSA key material for JWT signing (identity) and verification (all services).
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtRsaProperties.class)
public class PlatformRsaKeyProvider {

    private static final Set<String> LOCAL_PROFILES = Set.of("local", "test", "default");

    private final JwtRsaProperties properties;
    private final Environment environment;

    @Getter
    private RSAPublicKey publicKey;

    @Getter
    private RSAPrivateKey privateKey;

    @Getter
    private String keyId;

    @PostConstruct
    void init() {
        this.keyId = properties.getKeyId();
        this.publicKey = resolvePublicKey();
        if (properties.isSigningEnabled()) {
            if (!StringUtils.hasText(properties.getPrivateKeyLocation())) {
                throw new IllegalStateException(
                        "aisales.security.jwt.private-key-location is required when signing is enabled");
            }
            rejectClasspathDevPrivateKeyOutsideLocal(properties.getPrivateKeyLocation());
            this.privateKey = PemKeyLoader.loadPrivateKey(properties.getPrivateKeyLocation());
        }
    }

    /**
     * Committed {@code local-private.pem} is for local/test only. Non-local profiles must inject
     * keys via env/secret manager.
     */
    private void rejectClasspathDevPrivateKeyOutsideLocal(String location) {
        String normalized = location.toLowerCase(Locale.ROOT);
        if (!normalized.contains("local-private.pem") && !normalized.contains("classpath:jwt/local-private")) {
            return;
        }
        boolean localProfile = Arrays.stream(environment.getActiveProfiles())
                .map(p -> p.toLowerCase(Locale.ROOT))
                .anyMatch(LOCAL_PROFILES::contains);
        if (!localProfile && environment.getActiveProfiles().length == 0) {
            // No active profile often means default/local boot; allow for developer convenience.
            return;
        }
        if (!localProfile) {
            throw new IllegalStateException(
                    "Classpath JWT private key (local-private.pem) is not allowed outside local/test profiles. "
                            + "Set JWT_PRIVATE_KEY_PEM / aisales.security.jwt.private-key-location from a secret store.");
        }
    }

    private RSAPublicKey resolvePublicKey() {
        if (StringUtils.hasText(properties.getPublicKeyLocation())) {
            return PemKeyLoader.loadPublicKey(properties.getPublicKeyLocation());
        }
        if (StringUtils.hasText(properties.getJwkSetUri())) {
            return loadPublicKeyFromJwks(properties.getJwkSetUri(), properties.getKeyId());
        }
        // Local/dev fallback — committed public key in common-core / common-security.
        return PemKeyLoader.loadPublicKey("classpath:jwt/local-public.pem");
    }

    private static RSAPublicKey loadPublicKeyFromJwks(String jwkSetUri, String preferredKid) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(jwkSetUri))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("JWKS fetch failed with HTTP " + response.statusCode());
            }
            JWKSet jwkSet = JWKSet.parse(response.body());
            JWK jwk = null;
            if (StringUtils.hasText(preferredKid)) {
                jwk = jwkSet.getKeyByKeyId(preferredKid);
            }
            if (jwk == null && !jwkSet.getKeys().isEmpty()) {
                jwk = jwkSet.getKeys().get(0);
            }
            if (!(jwk instanceof RSAKey rsaKey)) {
                throw new IllegalStateException("JWKS at " + jwkSetUri + " does not contain an RSA key");
            }
            return rsaKey.toRSAPublicKey();
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load public key from JWKS: " + jwkSetUri, ex);
        }
    }

    public Map<String, Object> jwks() {
        RSAKey.Builder builder = new RSAKey.Builder(publicKey)
                .keyID(keyId)
                .keyUse(com.nimbusds.jose.jwk.KeyUse.SIGNATURE)
                .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256);
        return new JWKSet(builder.build()).toJSONObject();
    }

    public void requirePrivateKey() {
        if (privateKey == null) {
            throw new IllegalStateException("RSA private key is not available (signing disabled)");
        }
    }
}
