package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CatalogRecommendationGeneratedEvent extends BaseEvent {

    private String leadId;
    private String recommendationCount;
    private String topProductId;
    private String confidence;

    public static CatalogRecommendationGeneratedEvent of(
            String tenantId,
            String aggregateId,
            String leadId,
            String recommendationCount,
            String topProductId,
            String confidence,
            String correlationId) {
        CatalogRecommendationGeneratedEvent event = new CatalogRecommendationGeneratedEvent();
        event.init("CatalogRecommendationGenerated", tenantId, aggregateId, correlationId);
        event.leadId = leadId;
        event.recommendationCount = recommendationCount;
        event.topProductId = topProductId;
        event.confidence = confidence;
        return event;
    }
}
