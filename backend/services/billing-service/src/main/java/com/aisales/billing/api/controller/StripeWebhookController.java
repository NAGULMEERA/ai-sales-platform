package com.aisales.billing.api.controller;

import com.aisales.billing.application.service.StripeWebhookService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public Stripe webhook endpoint (signature-verified). Completes PENDING payments when
 * {@code payment_intent.succeeded} is received (client-side confirm path).
 */
@RestController
@RequestMapping("/api/v1/payments/webhooks")
@RequiredArgsConstructor
@Hidden
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    @PostMapping("/stripe")
    public ResponseEntity<Void> stripe(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature) {
        stripeWebhookService.handle(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }
}
