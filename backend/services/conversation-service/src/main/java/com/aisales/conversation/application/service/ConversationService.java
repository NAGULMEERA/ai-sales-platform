package com.aisales.conversation.application.service;

import com.aisales.common.contracts.conversation.AddMessageRequest;
import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.conversation.ConversationStatus;
import com.aisales.common.contracts.conversation.CreateConversationRequest;
import com.aisales.common.contracts.conversation.MessageSenderType;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.ConversationClosedEvent;
import com.aisales.common.events.model.ConversationMessageAddedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.conversation.application.mapper.ConversationMapper;
import com.aisales.conversation.domain.entity.ConversationMessage;
import com.aisales.conversation.domain.entity.ConversationThread;
import com.aisales.conversation.infrastructure.persistence.ConversationMessageRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationThreadRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationThreadRepository threadRepository;
    private final ConversationMessageRepository messageRepository;
    private final ConversationMapper mapper;
    private final EventPublisher eventPublisher;

    @Transactional
    public ConversationDto start(CreateConversationRequest request) {
        if (request.getLeadId() == null && request.getCustomerId() == null) {
            throw new ValidationException("Conversation requires leadId or customerId");
        }
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();

        ConversationThread thread = ConversationThread.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .leadId(request.getLeadId())
                .customerId(request.getCustomerId())
                .channel(request.getChannel())
                .subject(trimToNull(request.getSubject()))
                .status(ConversationStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        ConversationThread saved = threadRepository.saveAndFlush(thread);
        eventPublisher.publish(ConversationStartedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                uuidStr(saved.getLeadId()),
                uuidStr(saved.getCustomerId()),
                saved.getChannel().name(),
                saved.getStatus().name(),
                correlationId()));

        if (StringUtils.hasText(request.getInitialMessage())) {
            addMessageInternal(saved, AddMessageRequest.builder()
                    .senderType(MessageSenderType.CUSTOMER)
                    .senderId(actor)
                    .body(request.getInitialMessage().trim())
                    .build());
        }
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public ConversationDto get(UUID conversationId) {
        return mapper.toDto(requireThread(conversationId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationDto> list(UUID leadId, int page, int size) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<ConversationThread> result = leadId != null
                ? threadRepository.findByTenantIdAndLeadIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        tenantId, leadId, pageable)
                : threadRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId, pageable);
        return PageResponse.<ConversationDto>builder()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public ConversationMessageDto addMessage(UUID conversationId, AddMessageRequest request) {
        ConversationThread thread = requireThread(conversationId);
        return addMessageInternal(thread, request);
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageDto> listMessages(UUID conversationId) {
        requireThread(conversationId);
        return messageRepository
                .findByTenantIdAndConversationIdOrderByCreatedAtAsc(requireTenantId(), conversationId)
                .stream()
                .map(mapper::toMessageDto)
                .toList();
    }

    @Transactional
    public ConversationDto close(UUID conversationId, String reason) {
        ConversationThread thread = requireThread(conversationId);
        UUID actor = actorId();
        thread.close(actor);
        ConversationThread saved = threadRepository.save(thread);
        eventPublisher.publish(ConversationClosedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                uuidStr(saved.getLeadId()),
                StringUtils.hasText(reason) ? reason.trim() : "closed",
                correlationId()));
        return mapper.toDto(saved);
    }

    private ConversationMessageDto addMessageInternal(ConversationThread thread, AddMessageRequest request) {
        thread.assertOpen();
        UUID actor = actorId();
        Instant now = Instant.now();
        ConversationMessage message = ConversationMessage.builder()
                .tenantId(thread.getTenantId())
                .conversationId(thread.getId())
                .senderType(request.getSenderType())
                .senderId(request.getSenderId() != null ? request.getSenderId() : actor)
                .body(request.getBody().trim())
                .createdAt(now)
                .createdBy(actor)
                .build();
        ConversationMessage saved = messageRepository.saveAndFlush(message);
        thread.touch(actor);
        threadRepository.save(thread);

        eventPublisher.publish(ConversationMessageAddedEvent.of(
                thread.getTenantId().toString(),
                thread.getId().toString(),
                uuidStr(thread.getLeadId()),
                saved.getId().toString(),
                saved.getSenderType().name(),
                correlationId()));
        return mapper.toMessageDto(saved);
    }

    private ConversationThread requireThread(UUID conversationId) {
        return threadRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private UUID actorId() {
        return parseUuidOrNull(TenantContext.getUserId());
    }

    private String correlationId() {
        return CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String uuidStr(UUID value) {
        return value == null ? null : value.toString();
    }
}
