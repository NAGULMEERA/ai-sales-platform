package com.aisales.common.events.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConversationClosedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "ConversationClosed";

    private String leadId;
    private String reason;

    public static ConversationClosedEvent of(String tenantId, String conversationId, String leadId,
                                             String reason, String correlationId) {
        ConversationClosedEvent event = new ConversationClosedEvent();
        event.init(EVENT_TYPE, tenantId, conversationId, correlationId);
        event.leadId = leadId;
        event.reason = reason;
        return event;
    }
}
