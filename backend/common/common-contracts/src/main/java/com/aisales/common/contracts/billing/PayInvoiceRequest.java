package com.aisales.common.contracts.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayInvoiceRequest {

    /**
     * Optional Stripe PaymentMethod id (e.g. {@code pm_card_visa} in test mode).
     * Ignored by STUB provider.
     */
    private String paymentMethodId;
}
