package com.aisales.common.contracts.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tenant-configurable relative weights for hybrid catalog ranking. Defaults sum to 100.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogScoringWeights {

    @Builder.Default
    private int budget = 20;

    @Builder.Default
    private int location = 15;

    @Builder.Default
    private int feature = 20;

    @Builder.Default
    private int availability = 15;

    @Builder.Default
    private int leadIntent = 10;

    @Builder.Default
    private int conversation = 5;

    @Builder.Default
    private int preferences = 5;

    @Builder.Default
    private int aiSimilarity = 10;

    public static CatalogScoringWeights defaults() {
        return CatalogScoringWeights.builder().build();
    }
}
