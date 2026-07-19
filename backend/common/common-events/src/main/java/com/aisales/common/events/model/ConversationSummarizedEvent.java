package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConversationSummarizedEvent extends BaseEvent {

    private String conversationId;
    private String leadId;
    private String executionId;
    private String sentiment;
    private String intent;

    public static ConversationSummarizedEvent of(
            String tenantId,
            String conversationId,
            String leadId,
            String executionId,
            String sentiment,
            String intent,
            String correlationId) {
        ConversationSummarizedEvent event = new ConversationSummarizedEvent();
        event.init("ConversationSummarized", tenantId, conversationId, correlationId);
        event.conversationId = conversationId;
        event.leadId = leadId;
        event.executionId = executionId;
        event.sentiment = sentiment;
        event.intent = intent;
        return event;
    }
}
