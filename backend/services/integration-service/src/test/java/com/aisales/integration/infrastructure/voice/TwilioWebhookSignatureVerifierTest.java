package com.aisales.integration.infrastructure.voice;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.common.exception.exception.UnauthorizedException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TwilioWebhookSignatureVerifierTest {

    private static final String URL = "https://example.com/api/v1/integrations/webhooks/twilio/voice/status";
    private static final String AUTH_TOKEN = "test_auth_token";

    @Test
    void shouldAcceptValidSignature() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("CallSid", "CA123");
        params.put("CallStatus", "completed");
        String signature = TwilioWebhookSignatureVerifier.computeSignature(URL, params, AUTH_TOKEN);

        assertThatCode(() -> TwilioWebhookSignatureVerifier.verify(URL, params, signature, AUTH_TOKEN, true))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectTamperedParameter() {
        Map<String, String> signedParams = new LinkedHashMap<>();
        signedParams.put("CallSid", "CA123");
        signedParams.put("CallStatus", "completed");
        String signature = TwilioWebhookSignatureVerifier.computeSignature(URL, signedParams, AUTH_TOKEN);

        Map<String, String> tamperedParams = new LinkedHashMap<>();
        tamperedParams.put("CallSid", "CA123");
        tamperedParams.put("CallStatus", "failed");

        assertThatThrownBy(() ->
                        TwilioWebhookSignatureVerifier.verify(URL, tamperedParams, signature, AUTH_TOKEN, true))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldRejectMissingSignatureWhenTokenConfigured() {
        assertThatThrownBy(() ->
                        TwilioWebhookSignatureVerifier.verify(URL, Map.of(), null, AUTH_TOKEN, true))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldAllowMissingTokenWhenNotRequired() {
        assertThatCode(() -> TwilioWebhookSignatureVerifier.verify(URL, Map.of(), null, "", false))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectMissingTokenWhenRequired() {
        assertThatThrownBy(() -> TwilioWebhookSignatureVerifier.verify(URL, Map.of(), "sig", "", true))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldRejectMissingCallbackUrlWhenTokenConfigured() {
        assertThatThrownBy(() ->
                        TwilioWebhookSignatureVerifier.verify("", Map.of(), "sig", AUTH_TOKEN, true))
                .isInstanceOf(UnauthorizedException.class);
    }
}
