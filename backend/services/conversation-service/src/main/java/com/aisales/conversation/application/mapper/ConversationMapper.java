package com.aisales.conversation.application.mapper;

import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.conversation.domain.entity.ConversationMessage;
import com.aisales.conversation.domain.entity.ConversationThread;
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
                .channel(thread.getChannel())
                .subject(thread.getSubject())
                .status(thread.getStatus())
                .createdAt(thread.getCreatedAt())
                .updatedAt(thread.getUpdatedAt())
                .closedAt(thread.getClosedAt())
                .version(thread.getVersion())
                .build();
    }

    public ConversationMessageDto toMessageDto(ConversationMessage message) {
        return ConversationMessageDto.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderType(message.getSenderType())
                .senderId(message.getSenderId())
                .body(message.getBody())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
