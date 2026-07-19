package com.aisales.common.contracts.catalog;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Deterministic catalog matching foundation. AI ranking may enrich results later;
 * business services decide whether to act on matches.
 *
 * <p>Industry differences are expressed only via {@link #attributeFilters}
 * (plugin {@code catalogAttributeKeys}), not industry-specific DTOs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogMatchRequest {

    /** Optional lead correlation for audit / journey linkage (no FK). */
    private UUID leadId;

    private UUID customerId;

    @Size(max = 100)
    private String category;

    private CatalogProductType productType;

    @Size(max = 255)
    private String keyword;

    /** Budget ceiling; alias for maxPrice when set. */
    private BigDecimal budget;

    private BigDecimal maxPrice;

    @Size(max = 255)
    private String location;

    @Size(min = 3, max = 3)
    private String currency;

    /**
     * Industry-agnostic product attribute filters (equality / numeric compare).
     * Real Estate example: bedrooms, bathrooms, location.
     * Automobile example: make, model, year.
     */
    @Builder.Default
    private Map<String, Object> attributeFilters = new HashMap<>();

    /** Soft preferences used for scoring (do not hard-filter). */
    @Builder.Default
    private Map<String, Object> preferences = new HashMap<>();

    private CatalogScoringWeights scoringWeights;

    @Min(1)
    @Max(50)
    @Builder.Default
    private Integer limit = 10;
}
