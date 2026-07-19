package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MessageSentEvent extends BaseEvent {

    private String conversationId;
    private String leadId;
    private String messageId;
    private String senderType;
    private String channel;
    private String correlationKey;

    public static MessageSentEvent of(
            String tenantId,
            String conversationId,
            String leadId,
            String messageId,
            String senderType,
            String channel,
            String correlationKey,
            String correlationId) {
        MessageSentEvent event = new MessageSentEvent();
        event.init("MessageSent", tenantId, conversationId, correlationId);
        event.conversationId = conversationId;
        event.leadId = leadId;
        event.messageId = messageId;
        event.senderType = senderType;
        event.channel = channel;
        event.correlationKey = correlationKey;
        return event;
    }
}
