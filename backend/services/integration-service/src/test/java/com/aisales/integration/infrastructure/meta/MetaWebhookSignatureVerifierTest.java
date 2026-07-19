package com.aisales.integration.infrastructure.meta;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

class MetaWebhookSignatureVerifierTest {

    @Test
    void shouldAcceptValidSignature() {
        String secret = "app_secret";
        String payload = "{\"leadgenId\":\"1\"}";
        String sig = "sha256=" + MetaWebhookSignatureVerifier.hmacSha256Hex(secret, payload);

        assertThatCode(() -> MetaWebhookSignatureVerifier.verify(payload, sig, secret, true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAllowMissingSecretWhenNotRequired() {
        assertThatCode(() -> MetaWebhookSignatureVerifier.verify("{}", null, "", false))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectBadSignatureWhenRequired() {
        assertThatThrownBy(() -> MetaWebhookSignatureVerifier.verify("{}", "sha256=deadbeef", "secret", true))
                .isInstanceOf(UnauthorizedException.class);
    }
}
