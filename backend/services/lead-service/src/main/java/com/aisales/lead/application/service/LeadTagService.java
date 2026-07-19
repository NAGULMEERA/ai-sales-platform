package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.AddLeadTagRequest;
import com.aisales.common.contracts.lead.LeadTagDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.domain.entity.LeadTag;
import com.aisales.lead.domain.entity.LeadTagMapping;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import com.aisales.lead.infrastructure.persistence.LeadTagMappingRepository;
import com.aisales.lead.infrastructure.persistence.LeadTagRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class LeadTagService {

    private final LeadRepository leadRepository;
    private final LeadTagRepository tagRepository;
    private final LeadTagMappingRepository mappingRepository;
    private final LeadSideEffectRecorder sideEffects;

    @Transactional
    public LeadTagDto addTag(UUID leadId, AddLeadTagRequest request) {
        UUID tenantId = requireTenantId();
        requireLead(tenantId, leadId);
        String tagName = request.getTag().trim();
        LeadTag tag = tagRepository
                .findByTenantIdAndTagIgnoreCase(tenantId, tagName)
                .orElseGet(() -> tagRepository.save(LeadTag.builder()
                        .tenantId(tenantId)
                        .tag(tagName)
                        .color(trimToNull(request.getColor()))
                        .createdAt(Instant.now())
                        .build()));

        if (mappingRepository.existsByLeadIdAndTagId(leadId, tag.getId())) {
            return toDto(tag);
        }
        UUID actor = actorId();
        mappingRepository.save(LeadTagMapping.builder()
                .leadId(leadId)
                .tagId(tag.getId())
                .assignedBy(actor)
                .assignedAt(Instant.now())
                .build());
        sideEffects.recordActivity(leadId, "TAG_ADDED", "Tag " + tag.getTag(), actor);
        return toDto(tag);
    }

    @Transactional
    public void removeTag(UUID leadId, String tagName) {
        UUID tenantId = requireTenantId();
        requireLead(tenantId, leadId);
        if (!StringUtils.hasText(tagName)) {
            throw new ValidationException("tag is required");
        }
        LeadTag tag = tagRepository
                .findByTenantIdAndTagIgnoreCase(tenantId, tagName.trim())
                .orElseThrow(() -> new NotFoundException("Tag not found: " + tagName));
        if (!mappingRepository.existsByLeadIdAndTagId(leadId, tag.getId())) {
            throw new NotFoundException("Tag not mapped to lead: " + tagName);
        }
        mappingRepository.deleteByLeadIdAndTagId(leadId, tag.getId());
        sideEffects.recordActivity(leadId, "TAG_REMOVED", "Tag " + tag.getTag(), actorId());
    }

    @Transactional(readOnly = true)
    public List<LeadTagDto> listTags(UUID leadId) {
        UUID tenantId = requireTenantId();
        requireLead(tenantId, leadId);
        List<LeadTagMapping> mappings = mappingRepository.findByLeadIdOrderByAssignedAtDesc(leadId);
        if (mappings.isEmpty()) {
            return List.of();
        }
        List<UUID> tagIds = mappings.stream().map(LeadTagMapping::getTagId).distinct().toList();
        Map<UUID, LeadTag> tagsById = tagRepository.findByTenantIdAndIdIn(tenantId, tagIds).stream()
                .collect(Collectors.toMap(LeadTag::getId, t -> t, (a, b) -> a));
        return mappings.stream()
                .map(mapping -> tagsById.get(mapping.getTagId()))
                .filter(t -> t != null)
                .map(this::toDto)
                .toList();
    }

    private void requireLead(UUID tenantId, UUID leadId) {
        leadRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));
    }

    private LeadTagDto toDto(LeadTag tag) {
        return LeadTagDto.builder()
                .id(tag.getId())
                .tenantId(tag.getTenantId())
                .tag(tag.getTag())
                .color(tag.getColor())
                .createdAt(tag.getCreatedAt())
                .build();
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private UUID actorId() {
        String raw = TenantContext.getUserId();
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return UUID.fromString(raw);
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
