package com.aisales.billing.infrastructure.payment;

import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Fail-fast in production when stub payments are still selected, or when Stripe
 * client-side confirm is enabled without a webhook secret.
 */
@Component
@Profile("prod")
public class ProdPaymentGuard implements ApplicationRunner {

    private final String provider;
    private final PaymentProperties paymentProperties;

    public ProdPaymentGuard(
            @Value("${aisales.billing.payment.provider:STUB}") String provider,
            PaymentProperties paymentProperties) {
        this.provider = provider;
        this.paymentProperties = paymentProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (provider == null || "STUB".equalsIgnoreCase(provider.trim())) {
            throw new IllegalStateException(
                    "aisales.billing.payment.provider=STUB is forbidden when spring.profiles.active includes prod. "
                            + "Set aisales.billing.payment.provider=STRIPE "
                            + "(and aisales.billing.payment.stripe.enabled=true) before deploying.");
        }
        if ("STRIPE".equalsIgnoreCase(provider.trim())) {
            PaymentProperties.Stripe stripe = paymentProperties.getStripe();
            if (!stripe.isAutoConfirm() && !StringUtils.hasText(stripe.getWebhookSecret())) {
                throw new IllegalStateException(
                        "aisales.billing.payment.stripe.webhook-secret is required in prod when "
                                + "auto-confirm is false (client-side PaymentIntent confirmation).");
            }
        }
    }
}
