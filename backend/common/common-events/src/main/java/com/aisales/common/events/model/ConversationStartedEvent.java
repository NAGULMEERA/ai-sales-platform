package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConversationStartedEvent extends BaseEvent {

    private String leadId;
    private String customerId;
    private String channel;
    private String status;

    public static ConversationStartedEvent of(String tenantId, String conversationId, String leadId,
                                              String customerId, String channel, String status,
                                              String correlationId) {
        ConversationStartedEvent event = new ConversationStartedEvent();
        event.init("ConversationStarted", tenantId, conversationId, correlationId);
        event.leadId = leadId;
        event.customerId = customerId;
        event.channel = channel;
        event.status = status;
        return event;
    }
}
