package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.CreateLeadAttachmentRequest;
import com.aisales.common.contracts.lead.CreateLeadAttributionRequest;
import com.aisales.common.contracts.lead.CreateLeadCustomFieldRequest;
import com.aisales.common.contracts.lead.LeadAttachmentDto;
import com.aisales.common.contracts.lead.LeadAttributionDto;
import com.aisales.common.contracts.lead.LeadCustomFieldDto;
import com.aisales.common.contracts.lead.LeadQualityScoreDto;
import com.aisales.common.contracts.lead.RecordLeadQualityScoreRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadAttachment;
import com.aisales.lead.domain.entity.LeadAttribution;
import com.aisales.lead.domain.entity.LeadCustomFieldDefinition;
import com.aisales.lead.domain.entity.LeadQualityScore;
import com.aisales.lead.infrastructure.persistence.LeadAttachmentRepository;
import com.aisales.lead.infrastructure.persistence.LeadAttributionRepository;
import com.aisales.lead.infrastructure.persistence.LeadCustomFieldRepository;
import com.aisales.lead.infrastructure.persistence.LeadQualityScoreRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LeadExtensionService {

    private final LeadRepository leadRepository;
    private final LeadAttachmentRepository attachmentRepository;
    private final LeadCustomFieldRepository customFieldRepository;
    private final LeadAttributionRepository attributionRepository;
    private final LeadQualityScoreRepository qualityScoreRepository;
    private final LeadMapper leadMapper;
    private final LeadSideEffectRecorder sideEffects;

    @Transactional
    public LeadAttachmentDto addAttachment(UUID leadId, CreateLeadAttachmentRequest request) {
        requireLead(leadId);
        UUID actor = actorId();
        LeadAttachment saved = attachmentRepository.save(LeadAttachment.builder()
                .leadId(leadId)
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .fileType(request.getFileType())
                .fileSize(request.getFileSize())
                .uploadedBy(actor)
                .uploadedAt(Instant.now())
                .build());
        sideEffects.recordActivity(leadId, "ATTACHMENT_ADDED", request.getFileName(), actor);
        return leadMapper.toAttachmentDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LeadAttachmentDto> listAttachments(UUID leadId) {
        requireLead(leadId);
        return attachmentRepository.findByLeadIdOrderByUploadedAtDesc(leadId).stream()
                .map(leadMapper::toAttachmentDto).toList();
    }

    @Transactional
    public LeadCustomFieldDto createCustomField(CreateLeadCustomFieldRequest request) {
        UUID tenantId = requireTenantId();
        Instant now = Instant.now();
        LeadCustomFieldDefinition saved = customFieldRepository.save(LeadCustomFieldDefinition.builder()
                .tenantId(tenantId)
                .fieldName(request.getFieldName().trim())
                .fieldType(request.getFieldType().trim())
                .fieldOptions(request.getFieldOptions())
                .required(Boolean.TRUE.equals(request.getRequired()))
                .displayOrder(request.getDisplayOrder())
                .createdAt(now)
                .updatedAt(now)
                .build());
        return leadMapper.toCustomFieldDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LeadCustomFieldDto> listCustomFields() {
        return customFieldRepository.findByTenantIdOrderByDisplayOrderAscCreatedAtAsc(requireTenantId())
                .stream().map(leadMapper::toCustomFieldDto).toList();
    }

    @Transactional
    public LeadAttributionDto addAttribution(UUID leadId, CreateLeadAttributionRequest request) {
        requireLead(leadId);
        LeadAttribution saved = attributionRepository.save(LeadAttribution.builder()
                .leadId(leadId)
                .channel(request.getChannel().trim())
                .campaign(request.getCampaign())
                .adId(request.getAdId())
                .position(request.getPosition())
                .cost(request.getCost())
                .sourceDetails(request.getSourceDetails())
                .createdAt(Instant.now())
                .build());
        sideEffects.recordActivity(leadId, "ATTRIBUTION_ADDED", request.getChannel(), actorId());
        return leadMapper.toAttributionDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LeadAttributionDto> listAttributions(UUID leadId) {
        requireLead(leadId);
        return attributionRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(leadMapper::toAttributionDto).toList();
    }

    @Transactional
    public LeadQualityScoreDto recordQualityScore(UUID leadId, RecordLeadQualityScoreRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadQualityScore saved = qualityScoreRepository.save(LeadQualityScore.builder()
                .leadId(leadId)
                .overallScore(request.getOverallScore())
                .budgetFit(request.getBudgetFit())
                .timeline(request.getTimeline())
                .decisionMaker(request.getDecisionMaker())
                .competitorAwareness(request.getCompetitorAwareness())
                .objections(request.getObjections())
                .suggestedProduct(request.getSuggestedProduct())
                .nextAction(request.getNextAction())
                .rawLlmResponse(request.getRawLlmResponse())
                .scoredAt(Instant.now())
                .build());
        lead.setScore(request.getOverallScore());
        lead.setConfidenceScore(request.getOverallScore());
        lead.setUpdatedAt(Instant.now());
        lead.setUpdatedBy(actor);
        leadRepository.save(lead);
        sideEffects.recordActivity(leadId, "QUALITY_SCORED",
                "AI quality score " + request.getOverallScore(), actor);
        return leadMapper.toQualityScoreDto(saved);
    }

    @Transactional(readOnly = true)
    public List<LeadQualityScoreDto> listQualityScores(UUID leadId) {
        requireLead(leadId);
        return qualityScoreRepository.findByLeadIdOrderByScoredAtDesc(leadId).stream()
                .map(leadMapper::toQualityScoreDto).toList();
    }

    private Lead requireLead(UUID leadId) {
        UUID tenantId = requireTenantId();
        return leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));
    }

    private UUID requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(tenantId);
    }

    private UUID actorId() {
        String userId = TenantContext.getUserId();
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
