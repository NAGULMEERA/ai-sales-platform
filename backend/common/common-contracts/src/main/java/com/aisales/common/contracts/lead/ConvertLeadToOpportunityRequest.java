package com.aisales.common.contracts.lead;

import com.aisales.common.contracts.catalog.CatalogProductType;
import com.aisales.common.contracts.catalog.CatalogScoringWeights;
import jakarta.validation.constraints.DecimalMin;
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
 * Orchestrates Qualified Lead → Catalog Match → Recommendation → Opportunity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertLeadToOpportunityRequest {

    /** Existing customer; when omitted, lead conversion creates/resolves customer first. */
    private UUID customerId;

    @Size(max = 255)
    private String opportunityName;

    @Size(max = 100)
    private String category;

    private CatalogProductType productType;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal maxPrice;

    @Size(min = 3, max = 3)
    private String currency;

    @Builder.Default
    private Map<String, Object> attributeFilters = new HashMap<>();

    private CatalogScoringWeights scoringWeights;

    @Min(1)
    @Max(20)
    @Builder.Default
    private Integer matchLimit = 5;

    /** When true, creates opportunity even if no catalog match (amount null). */
    @Builder.Default
    private boolean allowWithoutMatch = false;

    @Size(max = 2000)
    private String notes;
}
