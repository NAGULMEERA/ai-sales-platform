package com.aisales.common.contracts.catalog;

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
public class CatalogOfferDto {

    private UUID id;
    private UUID tenantId;
    private UUID productId;
    private String code;
    private String name;
    private String currency;
    private BigDecimal unitPrice;
    private CatalogItemStatus status;
    private Instant validFrom;
    private Instant validTo;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
