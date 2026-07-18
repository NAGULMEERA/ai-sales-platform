package com.aisales.billing.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import com.aisales.billing.infrastructure.payment.StripeWebhookSignatureVerifier;
import com.aisales.billing.infrastructure.persistence.StripeWebhookEventRepository;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    private static final String SECRET = "whsec_test";

    @Mock private PaymentService paymentService;
    @Mock private StripeWebhookEventRepository stripeWebhookEventRepository;

    private StripeWebhookService service;
    private PaymentProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PaymentProperties();
        properties.getStripe().setWebhookSecret(SECRET);
        properties.getStripe().setWebhookToleranceSeconds(300);
        service = new StripeWebhookService(
                properties,
                paymentService,
                stripeWebhookEventRepository,
                new ObjectMapper(),
                new PlatformMetrics(new SimpleMeterRegistry()));
    }

    @Test
    void shouldCompleteOnPaymentIntentSucceeded() {
        String payload = """
                {"id":"evt_1","type":"payment_intent.succeeded","data":{"object":{"id":"pi_123"}}}
                """;
        String header = signed(payload);
        when(stripeWebhookEventRepository.insertIgnoreConflict(eq("evt_1"), eq("payment_intent.succeeded"), any()))
                .thenReturn(1);
        when(paymentService.completeSucceededByProviderPaymentId("pi_123")).thenReturn(true);

        service.handle(payload, header);

        verify(paymentService).completeSucceededByProviderPaymentId(eq("pi_123"));
    }

    @Test
    void shouldMarkFailedOnPaymentIntentFailed() {
        String payload = """
                {"id":"evt_2","type":"payment_intent.payment_failed","data":{"object":{"id":"pi_fail","last_payment_error":{"message":"card_declined"}}}}
                """;
        String header = signed(payload);
        when(stripeWebhookEventRepository.insertIgnoreConflict(eq("evt_2"), eq("payment_intent.payment_failed"), any()))
                .thenReturn(1);
        when(paymentService.markFailedByProviderPaymentId("pi_fail", "card_declined")).thenReturn(true);

        service.handle(payload, header);

        verify(paymentService).markFailedByProviderPaymentId("pi_fail", "card_declined");
    }

    @Test
    void shouldSkipDuplicateEvent() {
        String payload = """
                {"id":"evt_dup","type":"payment_intent.succeeded","data":{"object":{"id":"pi_123"}}}
                """;
        String header = signed(payload);
        when(stripeWebhookEventRepository.insertIgnoreConflict(eq("evt_dup"), eq("payment_intent.succeeded"), any()))
                .thenReturn(0);

        service.handle(payload, header);

        verifyNoInteractions(paymentService);
    }

    @Test
    void shouldIgnoreOtherEventTypesAfterClaim() {
        String payload = """
                {"id":"evt_3","type":"charge.succeeded","data":{"object":{"id":"ch_1"}}}
                """;
        String header = signed(payload);
        when(stripeWebhookEventRepository.insertIgnoreConflict(eq("evt_3"), eq("charge.succeeded"), any()))
                .thenReturn(1);

        service.handle(payload, header);

        verify(paymentService, never()).completeSucceededByProviderPaymentId(any());
        verify(paymentService, never()).markFailedByProviderPaymentId(any(), any());
    }

    private static String signed(String payload) {
        long ts = System.currentTimeMillis() / 1000L;
        String v1 = StripeWebhookSignatureVerifier.hmacSha256Hex(SECRET, ts + "." + payload);
        return "t=" + ts + ",v1=" + v1;
    }
}
