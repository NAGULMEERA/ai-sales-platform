package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConversationMessageAddedEvent extends BaseEvent {

    private String conversationId;
    private String leadId;
    private String messageId;
    private String senderType;

    public static ConversationMessageAddedEvent of(String tenantId, String conversationId, String leadId,
                                                   String messageId, String senderType,
                                                   String correlationId) {
        ConversationMessageAddedEvent event = new ConversationMessageAddedEvent();
        event.init("ConversationMessageAdded", tenantId, conversationId, correlationId);
        event.conversationId = conversationId;
        event.leadId = leadId;
        event.messageId = messageId;
        event.senderType = senderType;
        return event;
    }
}
