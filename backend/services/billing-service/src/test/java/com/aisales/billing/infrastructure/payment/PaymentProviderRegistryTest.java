package com.aisales.billing.infrastructure.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.billing.infrastructure.configuration.PaymentProperties;
import com.aisales.common.exception.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentProviderRegistryTest {

    @Test
    void shouldResolveStub() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("STUB");
        PaymentProviderRegistry registry =
                new PaymentProviderRegistry(List.of(new StubPaymentProvider()), properties);
        assertThat(registry.resolveDefault().name()).isEqualTo("STUB");
    }

    @Test
    void shouldFailWhenStripeMissing() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider("STRIPE");
        PaymentProviderRegistry registry =
                new PaymentProviderRegistry(List.of(new StubPaymentProvider()), properties);
        assertThatThrownBy(registry::resolveDefault)
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("STRIPE");
    }
}
