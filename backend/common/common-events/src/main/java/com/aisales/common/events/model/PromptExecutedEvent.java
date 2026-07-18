package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PromptExecutedEvent extends BaseEvent {

    private String promptCode;
    private String promptVersion;
    private String provider;
    private String model;
    private String confidence;
    private String businessReference;

    public static PromptExecutedEvent of(String tenantId, String executionId, String promptCode,
                                         String promptVersion, String provider, String model,
                                         String confidence, String businessReference,
                                         String correlationId) {
        PromptExecutedEvent event = new PromptExecutedEvent();
        event.init("PromptExecuted", tenantId, executionId, correlationId);
        event.promptCode = promptCode;
        event.promptVersion = promptVersion;
        event.provider = provider;
        event.model = model;
        event.confidence = confidence;
        event.businessReference = businessReference;
        return event;
    }
}
