package com.aisales.billing.infrastructure.payment;

import com.aisales.billing.domain.payment.PaymentProvider;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Instant-success payment for local/dev. Selected when
 * {@code aisales.billing.payment.provider=STUB}.
 */
@Component
@ConditionalOnProperty(name = "aisales.billing.payment.stub.enabled", havingValue = "true", matchIfMissing = true)
public class StubPaymentProvider implements PaymentProvider {

    public static final String NAME = "STUB";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public PaymentChargeResult charge(PaymentChargeRequest request) {
        return PaymentChargeResult.succeeded("stub_" + UUID.randomUUID());
    }
}
