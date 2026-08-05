package com.aisales.lead.application.service;

import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Projects conversation events onto the lead timeline (activities).
 * Conversation content remains owned by conversation-service.
 *
 * <p>Events arrive over Kafka (a shared, cross-tenant topic) carrying only a {@code leadId}.
 * Every projection is verified against {@code event.getTenantId()} before any write, so a
 * malformed or mismatched event can never record an activity onto another tenant's lead.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadConversationTimelineService {

    private final LeadSideEffectRecorder sideEffects;
    private final LeadRepository leadRepository;

    @Transactional
    public void onConversationStarted(String tenantId, String leadId, String conversationId, String channel) {
        UUID leadUuid = requireOwnedLead(tenantId, leadId);
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
    public void onMessageAdded(String tenantId, String leadId, String conversationId, String messageId,
                               String senderType) {
        UUID leadUuid = requireOwnedLead(tenantId, leadId);
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
    public void onConversationClosed(String tenantId, String leadId, String conversationId, String reason) {
        UUID leadUuid = requireOwnedLead(tenantId, leadId);
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

    /**
     * Parses the lead id and verifies it belongs to the event's tenant before any write.
     * Returns {@code null} (and logs) when the id is missing/invalid or ownership cannot be
     * confirmed, so the caller can skip the projection rather than write cross-tenant data.
     */
    private UUID requireOwnedLead(String tenantId, String leadId) {
        UUID leadUuid = parseLeadId(leadId);
        if (leadUuid == null) {
            return null;
        }
        UUID tenantUuid = parseTenantId(tenantId);
        if (tenantUuid == null) {
            log.warn("Conversation event for lead {} without a valid tenantId — skipping timeline projection",
                    leadUuid);
            return null;
        }
        if (leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantUuid, leadUuid).isEmpty()) {
            log.warn("Conversation event tenant {} does not own lead {} — skipping timeline projection",
                    tenantUuid, leadUuid);
            return null;
        }
        return leadUuid;
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

    private static UUID parseTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String nullToDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
