package com.aisales.billing.infrastructure.payment;

import com.aisales.billing.domain.payment.PaymentProvider;
import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Routes payments by {@code aisales.billing.payment.provider} ({@code STUB} | {@code STRIPE}).
 */
@Component
@RequiredArgsConstructor
public class PaymentProviderRegistry {

    private final List<PaymentProvider> providers;
    private final PaymentProperties properties;

    public PaymentProvider resolveDefault() {
        String configured = properties.getProvider();
        if (!StringUtils.hasText(configured)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "aisales.billing.payment.provider is not set");
        }
        String key = configured.trim().toUpperCase(Locale.ROOT);
        return providers.stream()
                .filter(p -> key.equals(p.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "No payment provider registered for aisales.billing.payment.provider="
                                + key
                                + ". Available: "
                                + providers.stream()
                                        .map(PaymentProvider::name)
                                        .sorted()
                                        .collect(Collectors.joining(", "))
                                + ". For STRIPE set aisales.billing.payment.stripe.enabled=true."));
    }
}
