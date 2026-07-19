package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiRecommendationGeneratedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "AIRecommendationGenerated";

    private String recommendationType;
    private String recommendation;
    private String confidence;
    private String businessReference;

    public static AiRecommendationGeneratedEvent of(
            String tenantId,
            String executionId,
            String recommendationType,
            String recommendation,
            String confidence,
            String businessReference,
            String correlationId) {
        AiRecommendationGeneratedEvent event = new AiRecommendationGeneratedEvent();
        event.init(EVENT_TYPE, tenantId, executionId, correlationId);
        event.recommendationType = recommendationType;
        event.recommendation = recommendation;
        event.confidence = confidence;
        event.businessReference = businessReference;
        return event;
    }
}
