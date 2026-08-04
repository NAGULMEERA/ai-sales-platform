package com.aisales.integration.infrastructure.voice;

import com.aisales.common.exception.exception.UnauthorizedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

/**
 * Verifies Twilio's {@code X-Twilio-Signature} header per Twilio's request-validation
 * algorithm: HMAC-SHA1(authToken, url + sorted "key"+"value" POST param pairs), base64-encoded.
 * The URL used must be the exact callback URL registered with Twilio (not a request-derived
 * URL, which can be altered by proxies/load balancers before signature comparison).
 *
 * @see <a href="https://www.twilio.com/docs/usage/security#validating-requests">Twilio request validation</a>
 */
public final class TwilioWebhookSignatureVerifier {

    private TwilioWebhookSignatureVerifier() {
    }

    public static void verify(String callbackUrl, Map<String, String> formParams,
            String signatureHeader, String authToken, boolean required) {
        if (!StringUtils.hasText(authToken)) {
            if (required) {
                throw new UnauthorizedException("Twilio auth token is not configured");
            }
            return;
        }
        if (!StringUtils.hasText(signatureHeader)) {
            throw new UnauthorizedException("Missing X-Twilio-Signature");
        }
        if (!StringUtils.hasText(callbackUrl)) {
            throw new UnauthorizedException(
                    "Twilio status-callback-url is not configured; cannot verify signature");
        }
        String expected = computeSignature(callbackUrl, formParams, authToken);
        if (!constantTimeEquals(expected, signatureHeader.trim())) {
            throw new UnauthorizedException("Invalid Twilio webhook signature");
        }
    }

    static String computeSignature(String url, Map<String, String> formParams, String authToken) {
        StringBuilder data = new StringBuilder(url);
        if (formParams != null && !formParams.isEmpty()) {
            new TreeMap<>(formParams).forEach((key, value) -> data.append(key).append(value == null ? "" : value));
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] hash = mac.doFinal(data.toString().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute Twilio webhook signature", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
