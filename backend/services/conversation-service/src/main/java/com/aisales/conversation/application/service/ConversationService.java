package com.aisales.conversation.application.service;

import com.aisales.common.contracts.conversation.AddMessageRequest;
import com.aisales.common.contracts.conversation.AddParticipantRequest;
import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationMessageDto;
import com.aisales.common.contracts.conversation.ConversationParticipantDto;
import com.aisales.common.contracts.conversation.ConversationParticipantRole;
import com.aisales.common.contracts.conversation.ConversationStatus;
import com.aisales.common.contracts.conversation.ConversationTimelineEntryDto;
import com.aisales.common.contracts.conversation.CreateConversationRequest;
import com.aisales.common.contracts.conversation.MessageContentType;
import com.aisales.common.contracts.conversation.MessageDeliveryStatus;
import com.aisales.common.contracts.conversation.MessageDirection;
import com.aisales.common.contracts.conversation.MessageSenderType;
import com.aisales.common.contracts.conversation.UpdateConversationMetadataRequest;
import com.aisales.common.contracts.conversation.UpdateMessageStatusRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.ConversationClosedEvent;
import com.aisales.common.events.model.ConversationMessageAddedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.common.events.model.MessageReceivedEvent;
import com.aisales.common.events.model.MessageSentEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.conversation.application.audit.ConversationAuditor;
import com.aisales.conversation.application.mapper.ConversationMapper;
import com.aisales.conversation.domain.channel.OutboundChannelPort;
import com.aisales.conversation.domain.entity.ConversationAttachment;
import com.aisales.conversation.domain.entity.ConversationMessage;
import com.aisales.conversation.domain.entity.ConversationParticipant;
import com.aisales.conversation.domain.entity.ConversationThread;
import com.aisales.conversation.domain.entity.ConversationTimelineEntry;
import com.aisales.conversation.infrastructure.persistence.ConversationAttachmentRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationMessageRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationParticipantRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationThreadRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationTimelineEntryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ConversationParticipantRepository participantRepository;
    private final ConversationAttachmentRepository attachmentRepository;
    private final ConversationTimelineEntryRepository timelineRepository;
    private final ConversationMapper mapper;
    private final EventPublisher eventPublisher;
    private final ConversationAuditor auditor;
    private final List<OutboundChannelPort> outboundChannels;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

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
                .opportunityId(request.getOpportunityId())
                .channel(request.getChannel())
                .subject(trimToNull(request.getSubject()))
                .status(ConversationStatus.OPEN)
                .metadata(request.getMetadata() == null ? new HashMap<>() : new HashMap<>(request.getMetadata()))
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        ConversationThread saved = threadRepository.saveAndFlush(thread);
        seedParticipants(saved, actor);
        appendTimeline(saved, "CONVERSATION_STARTED", "Conversation started", actor, Map.of(
                "channel", saved.getChannel().name()));

        eventPublisher.publish(ConversationStartedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                uuidStr(saved.getLeadId()),
                uuidStr(saved.getCustomerId()),
                saved.getChannel().name(),
                saved.getStatus().name(),
                correlationId()));
        auditor.conversationCreated(saved.getId());
        incrementMetric(MetricNames.CONVERSATION_STARTED, tenantId);

        if (StringUtils.hasText(request.getInitialMessage())) {
            addMessageInternal(saved, AddMessageRequest.builder()
                    .senderType(MessageSenderType.CUSTOMER)
                    .senderId(actor)
                    .body(request.getInitialMessage().trim())
                    .direction(MessageDirection.INBOUND)
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
        ConversationThread thread = requireThread(conversationId);
        UUID tenantId = thread.getTenantId();
        List<ConversationMessage> messages =
                messageRepository.findByTenantIdAndConversationIdOrderByCreatedAtAsc(tenantId, conversationId);
        Map<UUID, List<ConversationAttachment>> attachmentsByMessage =
                attachmentRepository
                        .findByTenantIdAndConversationIdOrderByCreatedAtAsc(tenantId, conversationId)
                        .stream()
                        .collect(Collectors.groupingBy(ConversationAttachment::getMessageId));
        return messages.stream()
                .map(message -> mapper.toMessageDto(
                        message,
                        attachmentsByMessage.getOrDefault(message.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ConversationMessageDto updateMessageStatus(
            UUID conversationId, UUID messageId, UpdateMessageStatusRequest request) {
        requireThread(conversationId);
        ConversationMessage message = messageRepository
                .findByTenantIdAndIdAndConversationId(requireTenantId(), messageId, conversationId)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));
        message.transitionStatus(request.getStatus(), request.getFailureReason());
        ConversationMessage saved = messageRepository.save(message);
        return mapper.toMessageDto(saved);
    }

    @Transactional
    public ConversationMessageDto retryMessage(UUID conversationId, UUID messageId) {
        ConversationThread thread = requireThread(conversationId);
        ConversationMessage message = messageRepository
                .findByTenantIdAndIdAndConversationId(requireTenantId(), messageId, conversationId)
                .orElseThrow(() -> new NotFoundException("Message not found: " + messageId));
        message.markRetry();
        ConversationMessage saved = messageRepository.save(message);
        if (saved.getDirection() == MessageDirection.OUTBOUND) {
            dispatchOutbound(thread, saved);
            saved.transitionStatus(MessageDeliveryStatus.SENT, null);
            saved = messageRepository.save(saved);
        }
        return mapper.toMessageDto(saved);
    }

    @Transactional
    public ConversationDto close(UUID conversationId, String reason) {
        ConversationThread thread = requireThread(conversationId);
        UUID actor = actorId();
        thread.close(actor);
        ConversationThread saved = threadRepository.save(thread);
        appendTimeline(saved, "CONVERSATION_CLOSED", "Conversation closed", actor, Map.of(
                "reason", StringUtils.hasText(reason) ? reason.trim() : "closed"));
        eventPublisher.publish(ConversationClosedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                uuidStr(saved.getLeadId()),
                StringUtils.hasText(reason) ? reason.trim() : "closed",
                correlationId()));
        auditor.conversationClosed(saved.getId());
        incrementMetric(MetricNames.CONVERSATION_CLOSED, saved.getTenantId());
        return mapper.toDto(saved);
    }

    @Transactional
    public ConversationDto updateMetadata(UUID conversationId, UpdateConversationMetadataRequest request) {
        ConversationThread thread = requireThread(conversationId);
        thread.replaceMetadata(request.getMetadata(), actorId());
        return mapper.toDto(threadRepository.save(thread));
    }

    @Transactional
    public ConversationParticipantDto addParticipant(UUID conversationId, AddParticipantRequest request) {
        ConversationThread thread = requireThread(conversationId);
        UUID actor = actorId();
        ConversationParticipant participant = ConversationParticipant.builder()
                .tenantId(thread.getTenantId())
                .conversationId(thread.getId())
                .role(request.getRole())
                .participantId(request.getParticipantId())
                .displayName(trimToNull(request.getDisplayName()))
                .joinedAt(Instant.now())
                .createdBy(actor)
                .build();
        ConversationParticipant saved = participantRepository.save(participant);
        appendTimeline(thread, "PARTICIPANT_ADDED", "Participant added", actor, Map.of(
                "role", request.getRole().name()));
        if (request.getRole() == ConversationParticipantRole.AGENT) {
            auditor.conversationAssigned(thread.getId());
        }
        return mapper.toParticipantDto(saved);
    }

    @Transactional(readOnly = true)
    public List<ConversationParticipantDto> listParticipants(UUID conversationId) {
        requireThread(conversationId);
        return participantRepository
                .findByTenantIdAndConversationIdAndLeftAtIsNullOrderByJoinedAtAsc(
                        requireTenantId(), conversationId)
                .stream()
                .map(mapper::toParticipantDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationTimelineEntryDto> timeline(UUID conversationId) {
        requireThread(conversationId);
        return timelineRepository
                .findByTenantIdAndConversationIdOrderByOccurredAtAsc(requireTenantId(), conversationId)
                .stream()
                .map(mapper::toTimelineDto)
                .toList();
    }

    @Transactional
    public void applyAiInsights(
            UUID conversationId,
            String aiSummary,
            String sentiment,
            String intent,
            String classification,
            String nextBestAction) {
        ConversationThread thread = requireThread(conversationId);
        UUID actor = actorId();
        thread.applyAiInsights(aiSummary, sentiment, intent, classification, nextBestAction, actor);
        threadRepository.save(thread);
        appendTimeline(thread, "AI_INSIGHTS_APPLIED", "AI insights applied", actor, Map.of(
                "intent", intent == null ? "" : intent,
                "sentiment", sentiment == null ? "" : sentiment));
    }

    @Transactional(readOnly = true)
    public ConversationThread requireThread(UUID conversationId) {
        return threadRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), conversationId)
                .orElseThrow(() -> new NotFoundException("Conversation not found: " + conversationId));
    }

    private ConversationMessageDto addMessageInternal(ConversationThread thread, AddMessageRequest request) {
        if (request.getSenderType() == null) {
            throw new ValidationException("senderType is required");
        }
        if (!StringUtils.hasText(request.getBody())) {
            throw new ValidationException("Message body is required");
        }
        thread.assertOpen();
        UUID actor = actorId();
        Instant now = Instant.now();
        MessageDirection direction = request.getDirection() != null
                ? request.getDirection()
                : inferDirection(request.getSenderType());
        MessageContentType contentType = request.getContentType() != null
                ? request.getContentType()
                : MessageContentType.TEXT;

        ConversationMessage message = ConversationMessage.builder()
                .tenantId(thread.getTenantId())
                .conversationId(thread.getId())
                .senderType(request.getSenderType())
                .senderId(request.getSenderId() != null ? request.getSenderId() : actor)
                .body(request.getBody().trim())
                .direction(direction)
                .deliveryStatus(MessageDeliveryStatus.PENDING)
                .contentType(contentType)
                .correlationId(StringUtils.hasText(request.getCorrelationId())
                        ? request.getCorrelationId().trim()
                        : correlationId())
                .mediaId(request.getMediaId())
                .mediaUrl(trimToNull(request.getMediaUrl()))
                .mediaContentType(trimToNull(request.getMediaContentType()))
                .createdAt(now)
                .createdBy(actor)
                .build();
        ConversationMessage saved = messageRepository.saveAndFlush(message);
        persistAttachments(thread, saved, request, actor);

        if (direction == MessageDirection.OUTBOUND) {
            dispatchOutbound(thread, saved);
            saved.transitionStatus(MessageDeliveryStatus.SENT, null);
            saved = messageRepository.save(saved);
            auditor.messageSent(saved.getId());
            incrementMetric(MetricNames.MESSAGE_SENT, thread.getTenantId());
            eventPublisher.publish(MessageSentEvent.of(
                    thread.getTenantId().toString(),
                    thread.getId().toString(),
                    uuidStr(thread.getLeadId()),
                    saved.getId().toString(),
                    saved.getSenderType().name(),
                    thread.getChannel().name(),
                    saved.getCorrelationId(),
                    correlationId()));
        } else {
            saved.transitionStatus(MessageDeliveryStatus.DELIVERED, null);
            saved = messageRepository.save(saved);
            auditor.messageReceived(saved.getId());
            incrementMetric(MetricNames.MESSAGE_RECEIVED, thread.getTenantId());
            eventPublisher.publish(MessageReceivedEvent.of(
                    thread.getTenantId().toString(),
                    thread.getId().toString(),
                    uuidStr(thread.getLeadId()),
                    saved.getId().toString(),
                    saved.getSenderType().name(),
                    thread.getChannel().name(),
                    saved.getCorrelationId(),
                    correlationId()));
        }

        thread.recordMessage(actor);
        threadRepository.save(thread);

        eventPublisher.publish(ConversationMessageAddedEvent.of(
                thread.getTenantId().toString(),
                thread.getId().toString(),
                uuidStr(thread.getLeadId()),
                saved.getId().toString(),
                saved.getSenderType().name(),
                correlationId()));

        appendTimeline(thread, direction == MessageDirection.INBOUND ? "MESSAGE_RECEIVED" : "MESSAGE_SENT",
                "Message " + direction.name().toLowerCase(), actor, Map.of(
                        "messageId", saved.getId().toString(),
                        "senderType", saved.getSenderType().name()));

        return mapper.toMessageDto(
                saved,
                attachmentRepository.findByTenantIdAndMessageIdOrderByCreatedAtAsc(
                        thread.getTenantId(), saved.getId()));
    }

    private void persistAttachments(
            ConversationThread thread,
            ConversationMessage message,
            AddMessageRequest request,
            UUID actor) {
        List<ConversationAttachment> attachments = new ArrayList<>();
        Instant now = Instant.now();
        if (request.getMediaId() != null) {
            attachments.add(ConversationAttachment.builder()
                    .tenantId(thread.getTenantId())
                    .conversationId(thread.getId())
                    .messageId(message.getId())
                    .mediaId(request.getMediaId())
                    .contentType(request.getMediaContentType())
                    .createdAt(now)
                    .createdBy(actor)
                    .build());
        }
        if (request.getAttachmentMediaIds() != null) {
            for (UUID mediaId : request.getAttachmentMediaIds()) {
                if (mediaId == null || mediaId.equals(request.getMediaId())) {
                    continue;
                }
                attachments.add(ConversationAttachment.builder()
                        .tenantId(thread.getTenantId())
                        .conversationId(thread.getId())
                        .messageId(message.getId())
                        .mediaId(mediaId)
                        .createdAt(now)
                        .createdBy(actor)
                        .build());
            }
        }
        if (!attachments.isEmpty()) {
            attachmentRepository.saveAll(attachments);
        }
    }

    private void dispatchOutbound(ConversationThread thread, ConversationMessage message) {
        outboundChannels.stream()
                .filter(port -> port.supports(thread.getChannel()))
                .findFirst()
                .ifPresent(port -> port.dispatch(thread, message));
    }

    private void seedParticipants(ConversationThread thread, UUID actor) {
        if (thread.getCustomerId() != null) {
            participantRepository.save(ConversationParticipant.builder()
                    .tenantId(thread.getTenantId())
                    .conversationId(thread.getId())
                    .role(ConversationParticipantRole.CUSTOMER)
                    .participantId(thread.getCustomerId())
                    .joinedAt(Instant.now())
                    .createdBy(actor)
                    .build());
        }
        if (actor != null) {
            participantRepository.save(ConversationParticipant.builder()
                    .tenantId(thread.getTenantId())
                    .conversationId(thread.getId())
                    .role(ConversationParticipantRole.AGENT)
                    .participantId(actor)
                    .joinedAt(Instant.now())
                    .createdBy(actor)
                    .build());
        }
    }

    private void appendTimeline(
            ConversationThread thread,
            String type,
            String summary,
            UUID actor,
            Map<String, Object> details) {
        timelineRepository.save(ConversationTimelineEntry.builder()
                .tenantId(thread.getTenantId())
                .conversationId(thread.getId())
                .entryType(type)
                .summary(summary)
                .actorId(actor)
                .occurredAt(Instant.now())
                .details(details == null ? new HashMap<>() : new HashMap<>(details))
                .build());
    }

    private static MessageDirection inferDirection(MessageSenderType senderType) {
        return senderType == MessageSenderType.CUSTOMER
                ? MessageDirection.INBOUND
                : MessageDirection.OUTBOUND;
    }

    private void incrementMetric(String name, UUID tenantId) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementForTenant(name, tenantId != null ? tenantId.toString() : null);
        }
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
