package com.aisales.common.core.security;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Loads RSA keys from PEM content or Spring resource locations (classpath:/file:).
 */
public final class PemKeyLoader {

    private PemKeyLoader() {
    }

    public static RSAPrivateKey loadPrivateKey(String pemOrLocation) {
        String pem = resolvePem(pemOrLocation);
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] decoded = Base64.getDecoder().decode(normalized);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA private key", ex);
        }
    }

    public static RSAPublicKey loadPublicKey(String pemOrLocation) {
        String pem = resolvePem(pemOrLocation);
        String normalized = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        try {
            byte[] decoded = Base64.getDecoder().decode(normalized);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load RSA public key", ex);
        }
    }

    private static String resolvePem(String pemOrLocation) {
        if (!StringUtils.hasText(pemOrLocation)) {
            throw new IllegalArgumentException("PEM content or resource location is required");
        }
        String trimmed = pemOrLocation.trim();
        if (trimmed.contains("BEGIN ")) {
            return trimmed;
        }
        Resource resource = new DefaultResourceLoader().getResource(trimmed);
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read PEM resource: " + trimmed, ex);
        }
    }
}
