package com.aisales.deal.application.service;

import com.aisales.common.contracts.deal.AssignOpportunityRequest;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.UpdateOpportunityRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.OpportunityAssignedEvent;
import com.aisales.common.events.model.OpportunityCreatedEvent;
import com.aisales.common.events.model.OpportunityStatusChangedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.deal.application.mapper.DealMapper;
import com.aisales.deal.domain.entity.Opportunity;
import com.aisales.deal.infrastructure.persistence.OpportunityRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final DealMapper mapper;
    private final EventPublisher eventPublisher;

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
            eventPublisher.publish(OpportunityStatusChangedEvent.of(
                    saved.getTenantId().toString(),
                    saved.getId().toString(),
                    saved.getCustomerId().toString(),
                    previous.name(),
                    saved.getStatus().name(),
                    "status update",
                    correlationId()));
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
        return mapper.toDto(saved);
    }

    @Transactional
    public void markQuoted(UUID opportunityId) {
        Opportunity opportunity = requireOpportunity(opportunityId);
        OpportunityStatus previous = opportunity.getStatus();
        opportunity.markQuoted(actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        if (saved.getStatus() != previous) {
            eventPublisher.publish(OpportunityStatusChangedEvent.of(
                    saved.getTenantId().toString(),
                    saved.getId().toString(),
                    saved.getCustomerId().toString(),
                    previous.name(),
                    saved.getStatus().name(),
                    "quote sent",
                    correlationId()));
        }
    }

    @Transactional
    public void markWon(UUID opportunityId, String reason) {
        Opportunity opportunity = requireOpportunity(opportunityId);
        OpportunityStatus previous = opportunity.getStatus();
        opportunity.transitionTo(OpportunityStatus.WON, actorId());
        Opportunity saved = opportunityRepository.save(opportunity);
        eventPublisher.publish(OpportunityStatusChangedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getCustomerId().toString(),
                previous.name(),
                saved.getStatus().name(),
                StringUtils.hasText(reason) ? reason : "quote accepted",
                correlationId()));
    }

    Opportunity requireOpportunity(UUID id) {
        return opportunityRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Opportunity not found: " + id));
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
}
