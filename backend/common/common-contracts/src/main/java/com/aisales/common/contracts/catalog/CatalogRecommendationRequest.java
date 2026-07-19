package com.aisales.common.contracts.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI-assisted catalog recommendation. Rule match runs first; optional AI similarity / ranking
 * enrich scores. Business services remain authoritative for acting on recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogRecommendationRequest {

    @NotNull
    @Valid
    private CatalogMatchRequest match;

    private UUID customerId;

    private UUID conversationId;

    private UUID knowledgeBaseId;

    /** Optional per-product AI similarity scores (0.0–1.0) from embeddings / AI Gateway. */
    @Builder.Default
    private Map<UUID, Double> aiSimilarityByProductId = new HashMap<>();

    /** Optional conversation / intent signals (metadata-driven). */
    @Builder.Default
    private Map<String, Object> conversationContext = new HashMap<>();

    @Builder.Default
    private Map<String, Object> customerPreferences = new HashMap<>();

    private CatalogScoringWeights scoringWeights;

    @Builder.Default
    private boolean includeAlternatives = true;
}
