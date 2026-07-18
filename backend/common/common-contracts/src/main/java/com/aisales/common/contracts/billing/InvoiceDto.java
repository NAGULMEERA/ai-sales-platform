package com.aisales.common.contracts.billing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDto {

    private UUID id;
    private UUID tenantId;
    private Instant periodStart;
    private Instant periodEnd;
    private InvoiceStatus status;
    private String currency;
    private String source;
    private BigDecimal subtotalUsd;
    private BigDecimal totalUsd;
    private Instant issuedAt;
    private Instant paidAt;
    private Instant createdAt;

    @Builder.Default
    private List<InvoiceLineItemDto> lineItems = new ArrayList<>();
}
