package com.aisales.conversation.application.audit;

import com.aisales.common.core.audit.Auditable;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ConversationAuditor {

    @Auditable(action = "CONVERSATION_CREATED", resourceType = "CONVERSATION")
    public void conversationCreated(UUID conversationId) {
    }

    @Auditable(action = "MESSAGE_SENT", resourceType = "CONVERSATION_MESSAGE")
    public void messageSent(UUID messageId) {
    }

    @Auditable(action = "MESSAGE_RECEIVED", resourceType = "CONVERSATION_MESSAGE")
    public void messageReceived(UUID messageId) {
    }

    @Auditable(action = "CONVERSATION_CLOSED", resourceType = "CONVERSATION")
    public void conversationClosed(UUID conversationId) {
    }

    @Auditable(action = "CONVERSATION_ESCALATED", resourceType = "CONVERSATION")
    public void conversationEscalated(UUID conversationId) {
    }

    @Auditable(action = "CONVERSATION_ASSIGNED", resourceType = "CONVERSATION")
    public void conversationAssigned(UUID conversationId) {
    }
}
