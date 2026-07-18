package com.aisales.billing.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

class StripeWebhookSignatureVerifierTest {

    private static final String SECRET = "whsec_test_secret";

    @Test
    void shouldAcceptValidSignature() {
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        long ts = System.currentTimeMillis() / 1000L;
        String v1 = StripeWebhookSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + payload);
        String header = "t=" + ts + ",v1=" + v1;

        assertThatCode(() -> StripeWebhookSignatureVerifier.verify(payload, header, SECRET, 300))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectTamperedPayload() {
        String payload = "{\"type\":\"payment_intent.succeeded\"}";
        long ts = System.currentTimeMillis() / 1000L;
        String v1 = StripeWebhookSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + payload);
        String header = "t=" + ts + ",v1=" + v1;

        assertThatThrownBy(() -> StripeWebhookSignatureVerifier.verify(
                        payload + " ", header, SECRET, 300))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("signature");
    }

    @Test
    void shouldRejectMissingSecret() {
        assertThatThrownBy(() -> StripeWebhookSignatureVerifier.verify("{}", "t=1,v1=abc", "", 300))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("secret");
    }
}
