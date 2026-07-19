package com.aisales.deal.application.service;

import com.aisales.common.contracts.deal.AddOpportunityNoteRequest;
import com.aisales.common.contracts.deal.AssignOpportunityRequest;
import com.aisales.common.contracts.deal.CloseOpportunityRequest;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.ReopenOpportunityRequest;
import com.aisales.common.contracts.deal.ScoreOpportunityRequest;
import com.aisales.common.contracts.deal.UpdateOpportunityRequest;
import com.aisales.common.contracts.deal.UpdateOpportunityStageRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.OpportunityAssignedEvent;
import com.aisales.common.events.model.OpportunityCreatedEvent;
import com.aisales.common.events.model.OpportunityLostEvent;
import com.aisales.common.events.model.OpportunityStatusChangedEvent;
import com.aisales.common.events.model.OpportunityWonEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.deal.application.mapper.DealMapper;
import com.aisales.deal.domain.entity.Opportunity;
import com.aisales.deal.domain.entity.OpportunityTimelineEntry;
import com.aisales.deal.infrastructure.persistence.OpportunityRepository;
import com.aisales.deal.infrastructure.persistence.OpportunityTimelineRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final OpportunityTimelineRepository timelineRepository;
    private final OpportunityTimelineRecorder timelineRecorder;
    private final DealMapper mapper;
    private final EventPublisher eventPublisher;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public OpportunityDto create(CreateOpportunityRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        UUID assignedTo = request.getAssignedTo() != null ? request.getAssignedTo() : actor;

        Opportunity opportunity = Opportunity.builder()
                .tenantId(tenantId)
                .organizationId(parseUuidOrNull(TenantContext.getOrganizationId()))
                .leadId(request.getLeadId())
                .customerId(request.getCustomerId())
                .name(request.getName().trim())
                .amount(request.getAmount())
                .currency(StringUtils.hasText(request.getCurrency())
                        ? request.getCurrency().trim().toUpperCase()
                        : "INR")
                .status(OpportunityStatus.OPEN)
                .probability(request.getProbability())
                .expectedCloseDate(request.getExpectedCloseDate())
                .assignedTo(assignedTo)
                .catalogProductId(request.getCatalogProductId())
                .catalogOfferId(request.getCatalogOfferId())
                .notes(trimToNull(request.getNotes()))
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        Opportunity saved = opportunityRepository.saveAndFlush(opportunity);
        eventPublisher.publish(OpportunityCreatedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                saved.getLeadId() != null ? saved.getLeadId().toString() : null,
                saved.getName(),
                saved.getStatus().name(),
                saved.getAssignedTo() != null ? saved.getAssignedTo().toString() : null,
                correlationId()));
        timelineRecorder.record(
                tenantId, saved.getId(), "OPPORTUNITY_CREATED", "Opportunity created", actor);
        if (saved.getCatalogProductId() != null) {
            timelineRecorder.record(
                    tenantId,
                    saved.getId(),
                    "CATALOG_MATCHED",
                    "Catalog product linked",
                    Map.of("productId", saved.getCatalogProductId().toString()),
                    actor);
        }
        incrementMetric(MetricNames.OPPORTUNITY_CREATED, tenantId);
        incrementMetric(MetricNames.OPPORTUNITY_CONVERSION, tenantId);
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public OpportunityDto get(UUID id) {
        return mapper.toDto(requireOpportunity(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OpportunityDto> list(
            int page, int size, OpportunityStatus status, UUID customerId, UUID leadId) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Opportunity> result = opportunityRepository.search(
                tenantId, status, customerId, leadId, PageRequest.of(safePage, safeSize));
        return PageResponse.<OpportunityDto>builder()
                .content(result.getContent().stream().map(mapper::toDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public List<OpportunityTimelineEntry> timeline(UUID id) {
        Opportunity opportunity = requireOpportunity(id);
        return timelineRepository.findByTenantIdAndOpportunityIdOrderByCreatedAtDesc(
                opportunity.getTenantId(), opportunity.getId());
    }

    @Transactional
    public OpportunityDto update(UUID id, UpdateOpportunityRequest request) {
        Opportunity opportunity = requireOpportunity(id);
        OpportunityStatus previous = opportunity.getStatus();
        opportunity.updateDetails(
                request.getName(),
                request.getAmount(),
                request.getCurrency(),
                request.getProbability(),
                request.getExpectedCloseDate(),
                request.getStatus(),
                actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        if (request.getStatus() != null && request.getStatus() != previous) {
            publishStatusChanged(saved, previous, "status update");
            timelineRecorder.record(
                    saved.getTenantId(),
                    saved.getId(),
                    "STAGE_CHANGED",
                    previous.name() + " → " + saved.getStatus().name(),
                    actorId());
        }
        return mapper.toDto(saved);
    }

    @Transactional
    public OpportunityDto updateStage(UUID id, UpdateOpportunityStageRequest request) {
        Opportunity opportunity = requireOpportunity(id);
        OpportunityStatus previous = opportunity.getStatus();
        opportunity.transitionTo(request.getStatus(), actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        if (saved.getStatus() != previous) {
            publishStatusChanged(
                    saved,
                    previous,
                    StringUtils.hasText(request.getReason()) ? request.getReason() : "stage update");
            timelineRecorder.record(
                    saved.getTenantId(),
                    saved.getId(),
                    "STAGE_CHANGED",
                    previous.name() + " → " + saved.getStatus().name(),
                    actorId());
            incrementMetric(MetricNames.OPPORTUNITY_STAGE_CHANGED, saved.getTenantId());
        }
        return mapper.toDto(saved);
    }

    @Transactional
    public OpportunityDto assign(UUID id, AssignOpportunityRequest request) {
        Opportunity opportunity = requireOpportunity(id);
        String previous = opportunity.getAssignedTo() != null
                ? opportunity.getAssignedTo().toString()
                : null;
        opportunity.assign(request.getAssignedTo(), actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        eventPublisher.publish(OpportunityAssignedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                saved.getAssignedTo().toString(),
                previous,
                correlationId()));
        timelineRecorder.record(
                saved.getTenantId(),
                saved.getId(),
                "ASSIGNED",
                "Assigned to " + saved.getAssignedTo(),
                actorId());
        return mapper.toDto(saved);
    }

    @Transactional
    public OpportunityDto addNote(UUID id, AddOpportunityNoteRequest request) {
        Opportunity opportunity = requireOpportunity(id);
        opportunity.addNote(request.getNote(), actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "NOTE_ADDED", "Note added", actorId());
        return mapper.toDto(saved);
    }

    @Transactional
    public OpportunityDto score(UUID id, ScoreOpportunityRequest request) {
        Opportunity opportunity = requireOpportunity(id);
        int overall = request.getOverallScore() != null
                ? request.getOverallScore()
                : averageComponents(request);
        opportunity.updateScore(overall, actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("overallScore", overall);
        timelineRecorder.record(
                saved.getTenantId(),
                saved.getId(),
                "SCORED",
                "Opportunity scored " + overall,
                details,
                actorId());
        return mapper.toDto(saved);
    }

    @Transactional
    public void markQuoted(UUID opportunityId) {
        Opportunity opportunity = requireOpportunity(opportunityId);
        OpportunityStatus previous = opportunity.getStatus();
        opportunity.markQuoted(actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        if (saved.getStatus() != previous) {
            publishStatusChanged(saved, previous, "quote sent");
            timelineRecorder.record(
                    saved.getTenantId(),
                    saved.getId(),
                    "STAGE_CHANGED",
                    previous.name() + " → " + saved.getStatus().name(),
                    actorId());
        }
    }

    @Transactional
    public OpportunityDto closeWon(UUID opportunityId, CloseOpportunityRequest request) {
        Opportunity opportunity = requireOpportunity(opportunityId);
        OpportunityStatus previous = opportunity.getStatus();
        String reason = request != null ? request.getReason() : null;
        opportunity.closeWon(reason, actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        publishStatusChanged(saved, previous, StringUtils.hasText(reason) ? reason : "won");
        eventPublisher.publish(OpportunityWonEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                saved.getLeadId() != null ? saved.getLeadId().toString() : null,
                saved.getCloseReason(),
                correlationId()));
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "WON", "Opportunity won", actorId());
        incrementMetric(MetricNames.OPPORTUNITY_WON, saved.getTenantId());
        return mapper.toDto(saved);
    }

    @Transactional
    public OpportunityDto closeLost(UUID opportunityId, CloseOpportunityRequest request) {
        Opportunity opportunity = requireOpportunity(opportunityId);
        OpportunityStatus previous = opportunity.getStatus();
        String reason = request != null ? request.getReason() : null;
        opportunity.closeLost(reason, actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        publishStatusChanged(saved, previous, StringUtils.hasText(reason) ? reason : "lost");
        eventPublisher.publish(OpportunityLostEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                saved.getLeadId() != null ? saved.getLeadId().toString() : null,
                saved.getCloseReason(),
                correlationId()));
        timelineRecorder.record(
                saved.getTenantId(), saved.getId(), "LOST", "Opportunity lost", actorId());
        incrementMetric(MetricNames.OPPORTUNITY_LOST, saved.getTenantId());
        return mapper.toDto(saved);
    }

    /** @deprecated prefer {@link #closeWon(UUID, CloseOpportunityRequest)} */
    @Transactional
    public void markWon(UUID opportunityId, String reason) {
        closeWon(opportunityId, CloseOpportunityRequest.builder().reason(reason).build());
    }

    @Transactional
    public OpportunityDto reopen(UUID id, ReopenOpportunityRequest request) {
        Opportunity opportunity = requireOpportunity(id);
        OpportunityStatus previous = opportunity.getStatus();
        OpportunityStatus target = request != null ? request.getStatus() : OpportunityStatus.OPEN;
        opportunity.reopen(target, actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        publishStatusChanged(
                saved,
                previous,
                request != null && StringUtils.hasText(request.getReason())
                        ? request.getReason()
                        : "reopened");
        timelineRecorder.record(
                saved.getTenantId(),
                saved.getId(),
                "REOPENED",
                previous.name() + " → " + saved.getStatus().name(),
                actorId());
        return mapper.toDto(saved);
    }

    @Transactional
    public void archive(UUID id) {
        Opportunity opportunity = requireOpportunity(id);
        opportunity.softDelete(actorId());
        opportunityRepository.save(opportunity);
        timelineRecorder.record(
                opportunity.getTenantId(),
                opportunity.getId(),
                "ARCHIVED",
                "Opportunity archived",
                actorId());
    }

    Opportunity requireOpportunity(UUID id) {
        return opportunityRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Opportunity not found: " + id));
    }

    private void publishStatusChanged(Opportunity saved, OpportunityStatus previous, String reason) {
        eventPublisher.publish(OpportunityStatusChangedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                previous.name(),
                saved.getStatus().name(),
                reason,
                correlationId()));
    }

    private static int averageComponents(ScoreOpportunityRequest request) {
        List<Integer> components = Stream.of(
                        request.getLeadScore(),
                        request.getCustomerScore(),
                        request.getCatalogMatchScore(),
                        request.getAiConfidenceScore(),
                        request.getConversationEngagementScore(),
                        request.getActivityScore(),
                        request.getPipelineStageScore())
                .filter(Objects::nonNull)
                .toList();
        if (components.isEmpty()) {
            throw new ValidationException("Provide overallScore or at least one component score");
        }
        return (int) Math.round(components.stream().mapToInt(Integer::intValue).average().orElseThrow());
    }

    private void incrementMetric(String name, UUID tenantId) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementForTenant(name, tenantId.toString());
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
}
