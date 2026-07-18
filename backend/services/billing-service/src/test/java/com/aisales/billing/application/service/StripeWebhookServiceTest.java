package com.aisales.billing.application.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import com.aisales.billing.infrastructure.payment.StripeWebhookSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    private static final String SECRET = "whsec_test";

    @Mock private PaymentService paymentService;

    private StripeWebhookService service;
    private PaymentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.getStripe().setWebhookSecret(SECRET);
        properties.getStripe().setWebhookToleranceSeconds(300);
        service = new StripeWebhookService(properties, paymentService, new ObjectMapper());
    }

    @Test
    void shouldCompleteOnPaymentIntentSucceeded() {
        String payload = """
                {"type":"payment_intent.succeeded","data":{"object":{"id":"pi_123"}}}
                """;
        long ts = System.currentTimeMillis() / 1000L;
        String v1 = StripeWebhookSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + payload);
        when(paymentService.completeSucceededByProviderPaymentId("pi_123")).thenReturn(true);

        service.handle(payload, "t=" + ts + ",v1=" + v1);

        verify(paymentService).completeSucceededByProviderPaymentId(eq("pi_123"));
    }

    @Test
    void shouldIgnoreOtherEventTypes() {
        String payload = """
                {"type":"charge.succeeded","data":{"object":{"id":"ch_1"}}}
                """;
        long ts = System.currentTimeMillis() / 1000L;
        String v1 = StripeWebhookSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + payload);

        service.handle(payload, "t=" + ts + ",v1=" + v1);

        verifyNoInteractions(paymentService);
    }
}
