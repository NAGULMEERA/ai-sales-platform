package com.aisales.common.contracts.catalog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configurable matching factor scores (0–100 each). Weights applied by ranking engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogMatchScoreFactorsDto {

    private int budgetMatch;
    private int locationMatch;
    private int featureMatch;
    private int availability;
    private int leadIntent;
    private int conversationContext;
    private int customerPreferences;
    private int aiSimilarity;
}
