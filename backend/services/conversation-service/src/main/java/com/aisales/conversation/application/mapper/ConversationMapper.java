package com.aisales.conversation.application.mapper;

import com.aisales.common.contracts.conversation.ConversationAttachmentDto;
import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.conversation.ConversationParticipantDto;
import com.aisales.common.contracts.conversation.ConversationTimelineEntryDto;
import com.aisales.conversation.domain.entity.ConversationAttachment;
import com.aisales.conversation.domain.entity.ConversationMessage;
import com.aisales.conversation.domain.entity.ConversationParticipant;
import com.aisales.conversation.domain.entity.ConversationThread;
import com.aisales.conversation.domain.entity.ConversationTimelineEntry;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ConversationMapper {

    public ConversationDto toDto(ConversationThread thread) {
        return ConversationDto.builder()
                .id(thread.getId())
                .tenantId(thread.getTenantId())
                .organizationId(thread.getOrganizationId())
                .leadId(thread.getLeadId())
                .customerId(thread.getCustomerId())
                .opportunityId(thread.getOpportunityId())
                .channel(thread.getChannel())
                .subject(thread.getSubject())
                .status(thread.getStatus())
                .summary(thread.getSummary())
                .aiSummary(thread.getAiSummary())
                .sentiment(thread.getSentiment())
                .intent(thread.getIntent())
                .classification(thread.getClassification())
                .nextBestAction(thread.getNextBestAction())
                .lastMessageAt(thread.getLastMessageAt())
                .createdAt(thread.getCreatedAt())
                .updatedAt(thread.getUpdatedAt())
                .closedAt(thread.getClosedAt())
                .version(thread.getVersion())
                .metadata(thread.getMetadata() == null ? new HashMap<>() : new HashMap<>(thread.getMetadata()))
                .build();
    }

    public ConversationMessageDto toMessageDto(ConversationMessage message) {
        return toMessageDto(message, List.of());
    }

    public ConversationMessageDto toMessageDto(
            ConversationMessage message, List<ConversationAttachment> attachments) {
        return ConversationMessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .body(message.getBody())
                .direction(message.getDirection())
                .deliveryStatus(message.getDeliveryStatus())
                .contentType(message.getContentType())
                .correlationId(message.getCorrelationId())
                .mediaId(message.getMediaId())
                .mediaUrl(message.getMediaUrl())
                .retryCount(message.getRetryCount())
                .failureReason(message.getFailureReason())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .attachments(attachments.stream().map(this::toAttachmentDto).toList())
                .build();
    }

    public ConversationParticipantDto toParticipantDto(ConversationParticipant participant) {
        return ConversationParticipantDto.builder()
                .id(participant.getId())
                .conversationId(participant.getConversationId())
                .role(participant.getRole())
                .participantId(participant.getParticipantId())
                .displayName(participant.getDisplayName())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .build();
    }

    public ConversationAttachmentDto toAttachmentDto(ConversationAttachment attachment) {
        return ConversationAttachmentDto.builder()
                .id(attachment.getId())
                .messageId(attachment.getMessageId())
                .mediaId(attachment.getMediaId())
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .sizeBytes(attachment.getSizeBytes())
                .createdAt(attachment.getCreatedAt())
                .build();
    }

    public ConversationTimelineEntryDto toTimelineDto(ConversationTimelineEntry entry) {
        return ConversationTimelineEntryDto.builder()
                .id(entry.getId())
                .entryType(entry.getEntryType())
                .summary(entry.getSummary())
                .actorId(entry.getActorId())
                .occurredAt(entry.getOccurredAt())
                .details(entry.getDetails() == null ? new HashMap<>() : new HashMap<>(entry.getDetails()))
                .build();
    }
}
