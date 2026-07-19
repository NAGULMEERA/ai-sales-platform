package com.aisales.billing.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment plug/flag: {@code aisales.billing.payment.provider} = {@code STUB} | {@code STRIPE}.
 */
@Data
@ConfigurationProperties(prefix = "aisales.billing.payment")
public class PaymentProperties {

    /** Active provider key. */
    private String provider = "STUB";

    private Stub stub = new Stub();

    private Stripe stripe = new Stripe();

    @Data
    public static class Stub {
        private boolean enabled = true;
    }

    @Data
    public static class Stripe {
        private boolean enabled = false;
        private String apiKey = "";
        private String baseUrl = "https://api.stripe.com";
        /**
         * When true, confirms PaymentIntents server-side (useful with test PaymentMethods).
         * Set false in prod when using client-side Elements.
         */
        private boolean autoConfirm = false;
        private String defaultPaymentMethod = "pm_card_visa";
        /** Stripe webhook signing secret ({@code whsec_...}). Required to verify {@code Stripe-Signature}. */
        private String webhookSecret = "";
        /** Max age of webhook timestamp before rejection (seconds). */
        private long webhookToleranceSeconds = 300;
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 30000;
    }
}
