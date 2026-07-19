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

    public static final String EVENT_TYPE = "PromptExecuted";

    private String promptCode;
    private String promptVersion;
    private String provider;
    private String model;
    private String confidence;
    private String businessReference;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    public static PromptExecutedEvent of(String tenantId, String executionId, String promptCode,
                                         String promptVersion, String provider, String model,
                                         String confidence, String businessReference,
                                         String correlationId) {
        return of(tenantId, executionId, promptCode, promptVersion, provider, model,
                confidence, businessReference, correlationId, null, null);
    }

    public static PromptExecutedEvent of(String tenantId, String executionId, String promptCode,
                                         String promptVersion, String provider, String model,
                                         String confidence, String businessReference,
                                         String correlationId,
                                         Integer promptTokens, Integer completionTokens) {
        PromptExecutedEvent event = new PromptExecutedEvent();
        event.init(EVENT_TYPE, tenantId, executionId, correlationId);
        event.promptCode = promptCode;
        event.promptVersion = promptVersion;
        event.provider = provider;
        event.model = model;
        event.confidence = confidence;
        event.businessReference = businessReference;
        event.promptTokens = promptTokens;
        event.completionTokens = completionTokens;
        if (promptTokens != null || completionTokens != null) {
            event.totalTokens = (promptTokens != null ? promptTokens : 0)
                    + (completionTokens != null ? completionTokens : 0);
        }
        return event;
    }
}
