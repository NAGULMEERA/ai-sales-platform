package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiQualificationCompletedEvent extends BaseEvent {

    private String promptCode;
    private String promptVersion;
    private String provider;
    private String model;
    private String recommendation;
    private String qualificationScore;
    private String confidenceScore;
    private String leadId;
    private boolean cacheHit;

    public static AiQualificationCompletedEvent of(
            String tenantId,
            String executionId,
            String promptCode,
            String promptVersion,
            String provider,
            String model,
            String recommendation,
            String qualificationScore,
            String confidenceScore,
            String leadId,
            boolean cacheHit,
            String correlationId) {
        AiQualificationCompletedEvent event = new AiQualificationCompletedEvent();
        event.init("AIQualificationCompleted", tenantId, executionId, correlationId);
        event.promptCode = promptCode;
        event.promptVersion = promptVersion;
        event.provider = provider;
        event.model = model;
        event.recommendation = recommendation;
        event.qualificationScore = qualificationScore;
        event.confidenceScore = confidenceScore;
        event.leadId = leadId;
        event.cacheHit = cacheHit;
        return event;
    }
}
