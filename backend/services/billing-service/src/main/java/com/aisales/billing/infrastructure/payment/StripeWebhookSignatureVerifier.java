package com.aisales.billing.infrastructure.payment;

import com.aisales.common.exception.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

/**
 * Verifies Stripe {@code Stripe-Signature} headers (t=, v1=) with HMAC-SHA256.
 */
public final class StripeWebhookSignatureVerifier {

    private StripeWebhookSignatureVerifier() {
    }

    public static void verify(String payload, String signatureHeader, String webhookSecret, long toleranceSeconds) {
        if (!StringUtils.hasText(webhookSecret)) {
            throw new UnauthorizedException("Stripe webhook secret is not configured");
        }
        if (!StringUtils.hasText(signatureHeader)) {
            throw new UnauthorizedException("Missing Stripe-Signature header");
        }
        if (payload == null) {
            throw new UnauthorizedException("Missing Stripe webhook payload");
        }

        String timestamp = null;
        String v1 = null;
        for (String part : signatureHeader.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            if ("t".equals(kv[0])) {
                timestamp = kv[1];
            } else if ("v1".equals(kv[0])) {
                v1 = kv[1];
            }
        }
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(v1)) {
            throw new UnauthorizedException("Invalid Stripe-Signature header");
        }

        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException("Invalid Stripe-Signature timestamp");
        }
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - ts) > toleranceSeconds) {
            throw new UnauthorizedException("Stripe webhook timestamp outside tolerance");
        }

        String signedPayload = timestamp + "." + payload;
        String expected = hmacSha256Hex(webhookSecret, signedPayload);
        if (!constantTimeEquals(expected, v1)) {
            throw new UnauthorizedException("Invalid Stripe webhook signature");
        }
    }

    public static String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute Stripe webhook signature", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] left = a.getBytes(StandardCharsets.UTF_8);
        byte[] right = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }
}
