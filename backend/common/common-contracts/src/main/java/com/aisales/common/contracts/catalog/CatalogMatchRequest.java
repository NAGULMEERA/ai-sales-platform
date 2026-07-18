package com.aisales.common.contracts.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deterministic catalog matching foundation. AI ranking may enrich results later;
 * business services decide whether to act on matches.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogMatchRequest {

    /** Optional lead correlation for audit / journey linkage (no FK). */
    private UUID leadId;

    @Size(max = 100)
    private String category;

    private CatalogProductType productType;

    @Size(max = 255)
    private String keyword;

    private BigDecimal maxPrice;

    @Size(min = 3, max = 3)
    private String currency;

    @Min(1)
    @Max(50)
    @Builder.Default
    private Integer limit = 10;
}
