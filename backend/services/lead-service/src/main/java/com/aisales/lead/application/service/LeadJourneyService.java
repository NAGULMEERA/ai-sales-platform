package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.ArchiveLeadRequest;
import com.aisales.common.contracts.lead.CancelLeadVisitRequest;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.LeadTimelineEntryDto;
import com.aisales.common.contracts.lead.ScheduleLeadVisitRequest;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.LeadArchivedEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import com.aisales.common.events.model.LeadVisitCancelledEvent;
import com.aisales.common.events.model.LeadVisitCompletedEvent;
import com.aisales.common.events.model.LeadVisitScheduledEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.service.LeadStateMachine;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Journey commands for visit and archive — keeps {@link LeadService} from becoming a god class.
 * AI / WhatsApp / calendar integrations must not call this; they emit events or call command APIs.
 */
@Service
@RequiredArgsConstructor
public class LeadJourneyService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository activityRepository;
    private final LeadMapper leadMapper;
    private final EventPublisher eventPublisher;
    private final LeadStateMachine stateMachine;
    private final LeadSideEffectRecorder sideEffects;

    @Transactional
    public LeadDto scheduleVisit(UUID leadId, ScheduleLeadVisitRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.scheduleVisit(actor, stateMachine);
        leadRepository.save(lead);
        String reason = request.getNotes() != null ? request.getNotes() : "Site visit scheduled";
        String corr = correlationId();
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), reason, actor);
        eventPublisher.publish(LeadVisitScheduledEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                request.getScheduledAt(), request.getLocation(), corr));
        eventPublisher.publish(LeadStatusChangedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                old.name(), lead.getStatus().name(), reason, corr));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto completeVisit(UUID leadId, String notes) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.completeVisit(actor, stateMachine);
        leadRepository.save(lead);
        String reason = StringUtils.hasText(notes) ? notes : "Site visit completed";
        String corr = correlationId();
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), reason, actor);
        eventPublisher.publish(LeadVisitCompletedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                notes, corr));
        eventPublisher.publish(LeadStatusChangedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                old.name(), lead.getStatus().name(), reason, corr));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto cancelVisit(UUID leadId, CancelLeadVisitRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.cancelVisit(actor, stateMachine);
        leadRepository.save(lead);
        String reason = request.getReason() != null ? request.getReason() : "Visit cancelled";
        String corr = correlationId();
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), reason, actor);
        eventPublisher.publish(LeadVisitCancelledEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                reason, corr));
        eventPublisher.publish(LeadStatusChangedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                old.name(), lead.getStatus().name(), reason, corr));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto archiveLead(UUID leadId, ArchiveLeadRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.archive(actor, stateMachine);
        leadRepository.save(lead);
        String reason = request.getReason() != null ? request.getReason() : "Archived";
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), reason, actor);
        eventPublisher.publish(LeadArchivedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                reason, correlationId()));
        return leadMapper.toDto(lead);
    }

    @Transactional(readOnly = true)
    public List<LeadTimelineEntryDto> timeline(UUID leadId) {
        requireLead(leadId);
        return activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(a -> LeadTimelineEntryDto.builder()
                        .id(a.getId())
                        .leadId(a.getLeadId())
                        .eventType(a.getActivityType())
                        .description(a.getDescription())
                        .actorId(a.getCreatedBy())
                        .occurredAt(a.getCreatedAt())
                        .build())
                .toList();
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

    private String correlationId() {
        return CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
    }
}
