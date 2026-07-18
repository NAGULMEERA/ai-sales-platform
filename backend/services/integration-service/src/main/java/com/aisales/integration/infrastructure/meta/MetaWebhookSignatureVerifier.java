package com.aisales.integration.infrastructure.meta;

import com.aisales.common.exception.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

/**
 * Verifies Meta {@code X-Hub-Signature-256} ({@code sha256=<hex>}).
 */
public final class MetaWebhookSignatureVerifier {

    private MetaWebhookSignatureVerifier() {
    }

    public static void verify(String payload, String signatureHeader, String appSecret, boolean required) {
        if (!StringUtils.hasText(appSecret)) {
            if (required) {
                throw new UnauthorizedException("Meta app secret is not configured");
            }
            return;
        }
        if (!StringUtils.hasText(signatureHeader) || !signatureHeader.startsWith("sha256=")) {
            throw new UnauthorizedException("Missing or invalid X-Hub-Signature-256");
        }
        String expected = "sha256=" + hmacSha256Hex(appSecret, payload == null ? "" : payload);
        if (!constantTimeEquals(expected, signatureHeader.trim())) {
            throw new UnauthorizedException("Invalid Meta webhook signature");
        }
    }

    public static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute Meta webhook signature", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
