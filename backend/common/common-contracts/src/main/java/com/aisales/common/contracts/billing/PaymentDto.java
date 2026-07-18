package com.aisales.common.contracts.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private UUID id;
    private UUID invoiceId;
    private UUID tenantId;
    private PaymentStatus status;
    private String provider;
    private String providerPaymentId;
    private String currency;
    private BigDecimal amountUsd;
    /** Present when Stripe requires client-side confirmation. */
    private String clientSecret;
    private Instant paidAt;
    private Instant createdAt;
}
