package com.aisales.billing.application.service;

import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import com.aisales.billing.infrastructure.payment.StripeWebhookSignatureVerifier;
import com.aisales.billing.infrastructure.persistence.StripeWebhookEventRepository;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Handles Stripe webhook events for PaymentIntent lifecycle (success / failure).
 * Deduplicates by Stripe {@code event.id} inside the same transaction as payment updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final PaymentProperties paymentProperties;
    private final PaymentService paymentService;
    private final StripeWebhookEventRepository stripeWebhookEventRepository;
    private final ObjectMapper objectMapper;
    private final PlatformMetrics platformMetrics;

    @Transactional
    public void handle(String payload, String stripeSignatureHeader) {
        PaymentProperties.Stripe stripe = paymentProperties.getStripe();
        StripeWebhookSignatureVerifier.verify(
                payload,
                stripeSignatureHeader,
                stripe.getWebhookSecret(),
                stripe.getWebhookToleranceSeconds());

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new ValidationException("Invalid Stripe webhook JSON");
        }

        String eventId = root.path("id").asText(null);
        String type = root.path("type").asText("");
        if (!StringUtils.hasText(eventId)) {
            throw new ValidationException("Stripe webhook missing event id");
        }

        int claimed = stripeWebhookEventRepository.insertIgnoreConflict(eventId, type, Instant.now());
        if (claimed == 0) {
            log.debug("Duplicate Stripe webhook ignored eventId={}", eventId);
            platformMetrics.increment(
                    MetricNames.BILLING_STRIPE_WEBHOOK, "type", type, "result", "duplicate");
            return;
        }

        String providerPaymentId = root.path("data").path("object").path("id").asText(null);
        switch (type) {
            case "payment_intent.succeeded" -> {
                handleSucceeded(providerPaymentId);
                platformMetrics.increment(
                        MetricNames.BILLING_STRIPE_WEBHOOK, "type", type, "result", "processed");
            }
            case "payment_intent.payment_failed" -> {
                handleFailed(root, providerPaymentId);
                platformMetrics.increment(
                        MetricNames.BILLING_STRIPE_WEBHOOK, "type", type, "result", "processed");
            }
            default -> {
                log.debug("Ignoring Stripe webhook type={} eventId={}", type, eventId);
                platformMetrics.increment(
                        MetricNames.BILLING_STRIPE_WEBHOOK, "type", type, "result", "ignored");
            }
        }
    }

    private void handleSucceeded(String providerPaymentId) {
        if (!StringUtils.hasText(providerPaymentId)) {
            log.warn("Stripe payment_intent.succeeded missing data.object.id");
            return;
        }
        boolean found = paymentService.completeSucceededByProviderPaymentId(providerPaymentId);
        if (!found) {
            log.warn("No local payment for Stripe PaymentIntent {}", providerPaymentId);
        } else {
            log.info("Completed payment for Stripe PaymentIntent {}", providerPaymentId);
        }
    }

    private void handleFailed(JsonNode root, String providerPaymentId) {
        if (!StringUtils.hasText(providerPaymentId)) {
            log.warn("Stripe payment_intent.payment_failed missing data.object.id");
            return;
        }
        String failureMessage = root.path("data")
                .path("object")
                .path("last_payment_error")
                .path("message")
                .asText(null);
        if (!StringUtils.hasText(failureMessage)) {
            failureMessage = root.path("data")
                    .path("object")
                    .path("last_payment_error")
                    .path("code")
                    .asText("payment_failed");
        }
        boolean found = paymentService.markFailedByProviderPaymentId(providerPaymentId, failureMessage);
        if (!found) {
            log.warn("No local payment for failed Stripe PaymentIntent {}", providerPaymentId);
        } else {
            log.info("Marked payment failed for Stripe PaymentIntent {}", providerPaymentId);
        }
    }
}
