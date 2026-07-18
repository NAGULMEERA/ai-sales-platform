package com.aisales.billing.domain.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pluggable payment backend. Selected by {@code aisales.billing.payment.provider}.
 */
public interface PaymentProvider {

    String name();

    PaymentChargeResult charge(PaymentChargeRequest request);

    record PaymentChargeRequest(
            UUID invoiceId,
            UUID tenantId,
            BigDecimal amountUsd,
            String currency,
            String paymentMethodId) {
    }

    record PaymentChargeResult(
            String providerPaymentId,
            boolean succeeded,
            boolean pendingClientAction,
            String clientSecret,
            String failureMessage) {

        public static PaymentChargeResult succeeded(String providerPaymentId) {
            return new PaymentChargeResult(providerPaymentId, true, false, null, null);
        }

        public static PaymentChargeResult pending(String providerPaymentId, String clientSecret) {
            return new PaymentChargeResult(providerPaymentId, false, true, clientSecret, null);
        }

        public static PaymentChargeResult failed(String providerPaymentId, String message) {
            return new PaymentChargeResult(providerPaymentId, false, false, null, message);
        }
    }
}
