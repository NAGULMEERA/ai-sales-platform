package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiReplyGeneratedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "AIReplyGenerated";

    private String conversationId;
    private String leadId;
    private String executionId;
    private String intent;
    private String confidence;
    private String nextBestAction;

    public static AiReplyGeneratedEvent of(
            String tenantId,
            String conversationId,
            String leadId,
            String executionId,
            String intent,
            String confidence,
            String nextBestAction,
            String correlationId) {
        AiReplyGeneratedEvent event = new AiReplyGeneratedEvent();
        event.init(EVENT_TYPE, tenantId, conversationId, correlationId);
        event.conversationId = conversationId;
        event.leadId = leadId;
        event.executionId = executionId;
        event.intent = intent;
        event.confidence = confidence;
        event.nextBestAction = nextBestAction;
        return event;
    }
}
