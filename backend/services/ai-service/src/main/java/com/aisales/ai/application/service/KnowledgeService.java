package com.aisales.ai.application.service;

import com.aisales.ai.application.mapper.AiMapper;
import com.aisales.ai.domain.entity.KnowledgeBase;
import com.aisales.ai.domain.entity.KnowledgeDocument;
import com.aisales.ai.infrastructure.persistence.KnowledgeBaseRepository;
import com.aisales.ai.infrastructure.persistence.KnowledgeDocumentRepository;
import com.aisales.common.contracts.ai.CreateKnowledgeBaseRequest;
import com.aisales.common.contracts.ai.KnowledgeBaseDto;
import com.aisales.common.contracts.ai.KnowledgeBaseStatus;
import com.aisales.common.contracts.ai.KnowledgeDocumentDto;
import com.aisales.common.contracts.ai.KnowledgeDocumentStatus;
import com.aisales.common.contracts.ai.RegisterKnowledgeDocumentRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.KnowledgeBaseCreatedEvent;
import com.aisales.common.events.model.KnowledgeDocumentRegisteredEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import java.time.Instant;
import java.util.HashMap;
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
public class KnowledgeService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final AiMapper mapper;
    private final EventPublisher eventPublisher;

    @Transactional
    public KnowledgeBaseDto createBase(CreateKnowledgeBaseRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        String code = request.getCode().trim().toUpperCase();

        if (knowledgeBaseRepository.existsByTenantIdAndCodeAndDeletedAtIsNull(tenantId, code)) {
            throw new ValidationException("Knowledge base code already exists: " + code);
        }

        KnowledgeBase kb = KnowledgeBase.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .code(code)
                .name(request.getName().trim())
                .description(trimToNull(request.getDescription()))
                .status(KnowledgeBaseStatus.ACTIVE)
                .metadata(request.getMetadata() != null
                        ? new HashMap<>(request.getMetadata())
                        : new HashMap<>())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        KnowledgeBase saved = knowledgeBaseRepository.saveAndFlush(kb);
        eventPublisher.publish(KnowledgeBaseCreatedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getCode(),
                saved.getName(),
                saved.getStatus().name(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseDto getBase(UUID id) {
        return mapper.toDto(requireBase(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeBaseDto> listBases(int page, int size) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<KnowledgeBase> result = knowledgeBaseRepository
                .findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
                        tenantId, PageRequest.of(safePage, safeSize));
        return PageResponse.<KnowledgeBaseDto>builder()
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
    public KnowledgeDocumentDto registerDocument(UUID knowledgeBaseId, RegisterKnowledgeDocumentRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        KnowledgeBase kb = requireBase(knowledgeBaseId);
        kb.assertActive();

        if (request.getMediaId() == null && !StringUtils.hasText(request.getObjectKey())) {
            throw new ValidationException("mediaId or objectKey is required");
        }

        KnowledgeDocument doc = KnowledgeDocument.builder()
                .tenantId(tenantId)
                .knowledgeBaseId(kb.getId())
                .name(request.getName().trim())
                .contentType(trimToNull(request.getContentType()))
                .sizeBytes(request.getSizeBytes())
                .mediaId(request.getMediaId())
                .objectKey(trimToNull(request.getObjectKey()))
                .status(KnowledgeDocumentStatus.PENDING)
                .metadata(request.getMetadata() != null
                        ? new HashMap<>(request.getMetadata())
                        : new HashMap<>())
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        KnowledgeDocument saved = knowledgeDocumentRepository.saveAndFlush(doc);
        eventPublisher.publish(KnowledgeDocumentRegisteredEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getKnowledgeBaseId().toString(),
                saved.getName(),
                saved.getStatus().name(),
                saved.getMediaId() != null ? saved.getMediaId().toString() : null,
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentDto> listDocuments(UUID knowledgeBaseId) {
        requireBase(knowledgeBaseId);
        return knowledgeDocumentRepository
                .findByTenantIdAndKnowledgeBaseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        requireTenantId(), knowledgeBaseId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentDto getDocument(UUID documentId) {
        return knowledgeDocumentRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), documentId)
                .map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("Knowledge document not found: " + documentId));
    }

    private KnowledgeBase requireBase(UUID id) {
        return knowledgeBaseRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Knowledge base not found: " + id));
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
}
