package com.aisales.lead.application.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Projects conversation events onto the lead timeline (activities).
 * Conversation content remains owned by conversation-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadConversationTimelineService {

    private final LeadSideEffectRecorder sideEffects;

    @Transactional
    public void onConversationStarted(String leadId, String conversationId, String channel) {
        UUID leadUuid = parseLeadId(leadId);
        if (leadUuid == null) {
            return;
        }
        sideEffects.recordActivity(
                leadUuid,
                "CONVERSATION_STARTED",
                "Conversation " + conversationId + " started on " + nullToDash(channel),
                null);
    }

    @Transactional
    public void onMessageAdded(String leadId, String conversationId, String messageId, String senderType) {
        UUID leadUuid = parseLeadId(leadId);
        if (leadUuid == null) {
            return;
        }
        sideEffects.recordActivity(
                leadUuid,
                "CONVERSATION_MESSAGE",
                "Message " + messageId + " (" + nullToDash(senderType) + ") on conversation "
                        + conversationId,
                null);
    }

    @Transactional
    public void onConversationClosed(String leadId, String conversationId, String reason) {
        UUID leadUuid = parseLeadId(leadId);
        if (leadUuid == null) {
            return;
        }
        sideEffects.recordActivity(
                leadUuid,
                "CONVERSATION_CLOSED",
                "Conversation " + conversationId + " closed"
                        + (StringUtils.hasText(reason) ? " (" + reason + ")" : ""),
                null);
    }

    private static UUID parseLeadId(String leadId) {
        if (!StringUtils.hasText(leadId)) {
            log.debug("Conversation event without leadId — skipping timeline projection");
            return null;
        }
        try {
            return UUID.fromString(leadId);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid leadId on conversation event: {}", leadId);
            return null;
        }
    }

    private static String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
