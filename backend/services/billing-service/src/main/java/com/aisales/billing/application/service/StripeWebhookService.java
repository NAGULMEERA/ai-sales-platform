package com.aisales.billing.application.service;

import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import com.aisales.billing.infrastructure.payment.StripeWebhookSignatureVerifier;
import com.aisales.common.exception.exception.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Handles Stripe webhook events used to complete PENDING PaymentIntents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final PaymentProperties paymentProperties;
    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

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

        String type = root.path("type").asText("");
        if (!"payment_intent.succeeded".equals(type)) {
            log.debug("Ignoring Stripe webhook type={}", type);
            return;
        }

        String providerPaymentId = root.path("data").path("object").path("id").asText(null);
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
}
