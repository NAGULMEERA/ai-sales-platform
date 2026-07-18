package com.aisales.lead.application.service;

import com.aisales.common.contracts.lead.AssignLeadRequest;
import com.aisales.common.contracts.lead.AssignmentStrategy;
import com.aisales.common.contracts.lead.ChangeLeadStatusRequest;
import com.aisales.common.contracts.lead.ConvertLeadRequest;
import com.aisales.common.contracts.lead.CreateLeadFollowupRequest;
import com.aisales.common.contracts.lead.CreateLeadNoteRequest;
import com.aisales.common.contracts.lead.CreateLeadRequest;
import com.aisales.common.contracts.lead.LeadActivityDto;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadDuplicateDto;
import com.aisales.common.contracts.lead.LeadFollowupDto;
import com.aisales.common.contracts.lead.LeadNoteDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.LeadStatusHistoryDto;
import com.aisales.common.contracts.lead.LoseLeadRequest;
import com.aisales.common.contracts.lead.QualifyLeadRequest;
import com.aisales.common.contracts.lead.ScoreLeadRequest;
import com.aisales.common.contracts.lead.UpdateLeadRequest;
import com.aisales.common.core.dto.PageResponse;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.LeadAssignedEvent;
import com.aisales.common.events.model.LeadContactedEvent;
import com.aisales.common.events.model.LeadConvertedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadLostEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.LeadScoredEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import com.aisales.common.events.model.LeadValidatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadAssignment;
import com.aisales.lead.domain.entity.LeadDuplicate;
import com.aisales.lead.domain.entity.LeadFollowup;
import com.aisales.lead.domain.entity.LeadNote;
import com.aisales.lead.domain.entity.LeadScoreRecord;
import com.aisales.lead.domain.service.LeadStateMachine;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadAssignmentRepository;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadFollowupRepository;
import com.aisales.lead.infrastructure.persistence.LeadNoteRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import com.aisales.lead.infrastructure.persistence.LeadScoreRepository;
import com.aisales.lead.infrastructure.persistence.LeadStatusHistoryRepository;
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
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadAssignmentRepository assignmentRepository;
    private final LeadNoteRepository noteRepository;
    private final LeadActivityRepository activityRepository;
    private final LeadFollowupRepository followupRepository;
    private final LeadScoreRepository scoreRepository;
    private final LeadStatusHistoryRepository statusHistoryRepository;
    private final LeadDuplicateRepository duplicateRepository;
    private final LeadMapper leadMapper;
    private final EventPublisher eventPublisher;
    private final LeadStateMachine stateMachine;
    private final LeadSideEffectRecorder sideEffects;
    private final DuplicateLeadDetectionService duplicateDetection;
    private final LeadAssignmentPoolService assignmentPoolService;
    private final PipelineService pipelineService;
    private final LeadCustomerConversionGateway customerConversionGateway;

    @Transactional
    public LeadDto createLead(CreateLeadRequest request) {
        UUID tenantId = requireTenantId();
        UUID actor = actorId();
        Instant now = Instant.now();
        UUID orgId = parseUuidOrNull(TenantContext.getOrganizationId());

        UUID pipelineId = pipelineService.resolvePipelineIdForCreate(tenantId, request.getPipelineId());

        Lead lead = Lead.builder()
                .tenantId(tenantId)
                .organizationId(orgId)
                .pipelineId(pipelineId)
                .customerName(request.getCustomerName().trim())
                .phone(request.getPhone().trim())
                .email(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null)
                .sourceType(request.getSourceType().trim())
                .sourceId(request.getSourceId())
                .campaign(request.getCampaign())
                .status(LeadStatus.NEW)
                .validated(false)
                .qualified(false)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();
        lead.assertHasContactMethod();

        Lead saved = leadRepository.saveAndFlush(lead);
        if (!StringUtils.hasText(saved.getExternalId())) {
            saved = leadRepository.findById(saved.getId()).orElse(saved);
        }
        duplicateDetection.detectAndRecord(saved);
        sideEffects.recordActivity(saved.getId(), "CREATED", "Lead captured", actor);

        eventPublisher.publish(LeadCreatedEvent.of(
                tenantId.toString(), saved.getId().toString(), saved.getCustomerName(),
                saved.getSourceType(), saved.getStatus().name(), correlationId()));
        return leadMapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public LeadDto getLead(UUID leadId) {
        return leadMapper.toDto(requireLead(leadId));
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadDto> listLeads(int page, int size, LeadStatus status,
                                           UUID assignedTo, String sourceType, String q) {
        UUID tenantId = requireTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<Lead> result = leadRepository.search(
                tenantId, status, assignedTo,
                StringUtils.hasText(sourceType) ? sourceType.trim() : null,
                StringUtils.hasText(q) ? q.trim() : null,
                pageable);
        return toPage(result);
    }

    @Transactional
    public LeadDto updateLead(UUID leadId, UpdateLeadRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        lead.updateDetails(request.getCustomerName(), request.getPhone(), request.getEmail(),
                request.getSourceType(), request.getSourceId(), request.getCampaign(), actor);
        sideEffects.recordActivity(lead.getId(), "UPDATED", "Lead details updated", actor);
        return leadMapper.toDto(leadRepository.save(lead));
    }

    @Transactional
    public void deleteLead(UUID leadId) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        lead.softDelete(actor);
        leadRepository.save(lead);
        sideEffects.recordActivity(leadId, "DELETED", "Lead soft-deleted", actor);
    }

    @Transactional
    public LeadDto validateLead(UUID leadId) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        lead.markValidated(actor);
        leadRepository.save(lead);
        sideEffects.recordActivity(leadId, "VALIDATED", "Lead validated", actor);
        eventPublisher.publish(LeadValidatedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(), correlationId()));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto assignLead(UUID leadId, AssignLeadRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        AssignmentStrategy strategy = request.getStrategy() != null
                ? request.getStrategy()
                : AssignmentStrategy.MANUAL;
        UUID assignee = resolveAssignee(lead.getTenantId(), strategy, request.getAssignedTo());
        String reason = request.getAssignmentReason() != null
                ? request.getAssignmentReason()
                : strategy.name();

        assignmentRepository.findFirstByLeadIdAndUnassignedAtIsNullOrderByAssignedAtDesc(leadId)
                .ifPresent(current -> {
                    current.setUnassignedAt(Instant.now());
                    assignmentRepository.save(current);
                });
        lead.assign(assignee, actor, stateMachine);
        leadRepository.save(lead);
        assignmentRepository.save(LeadAssignment.builder()
                .leadId(leadId)
                .assignedTo(assignee)
                .assignedAt(Instant.now())
                .assignmentReason(reason)
                .createdBy(actor)
                .build());
        sideEffects.recordActivity(leadId, "ASSIGNED",
                "Assigned to " + assignee + " via " + strategy, actor);
        eventPublisher.publish(LeadAssignedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                assignee.toString(), reason, correlationId()));
        return leadMapper.toDto(lead);
    }

    private UUID resolveAssignee(UUID tenantId, AssignmentStrategy strategy, UUID assignedTo) {
        if (strategy == AssignmentStrategy.ROUND_ROBIN) {
            return assignmentPoolService.nextRoundRobinAssignee(tenantId);
        }
        if (assignedTo == null) {
            throw new ValidationException("assignedTo is required for MANUAL assignment");
        }
        return assignedTo;
    }

    @Transactional
    public LeadDto qualifyLead(UUID leadId, QualifyLeadRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.qualify(request.getScore(), actor, stateMachine);
        leadRepository.save(lead);
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), request.getNotes(), actor);
        if (request.getScore() != null) {
            scoreRepository.save(LeadScoreRecord.builder()
                    .leadId(leadId)
                    .score(request.getScore())
                    .scoreType("QUALIFICATION")
                    .explanation(request.getNotes())
                    .scoredAt(Instant.now())
                    .scoredBy(actor)
                    .build());
        }
        eventPublisher.publish(LeadQualifiedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                lead.getScore(), lead.getStatus().name(), correlationId()));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto contactLead(UUID leadId, String channel) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.markContacted(actor, stateMachine);
        leadRepository.save(lead);
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), "contacted", actor);
        String contactChannel = StringUtils.hasText(channel) ? channel : "UNSPECIFIED";
        eventPublisher.publish(LeadContactedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                contactChannel, correlationId()));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto changeStatus(UUID leadId, ChangeLeadStatusRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.transitionTo(request.getStatus(), actor, stateMachine);
        leadRepository.save(lead);
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), request.getReason(), actor);
        eventPublisher.publish(LeadStatusChangedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                old.name(), lead.getStatus().name(), request.getReason(), correlationId()));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto scoreLead(UUID leadId, ScoreLeadRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        Instant now = Instant.now();
        persistComponentScore(leadId, "BUDGET", request.getBudgetScore(), request.getExplanation(), actor, now);
        persistComponentScore(leadId, "TIMELINE", request.getTimelineScore(), request.getExplanation(), actor, now);
        persistComponentScore(leadId, "LOCATION", request.getLocationScore(), request.getExplanation(), actor, now);
        persistComponentScore(leadId, "ENGAGEMENT", request.getEngagementScore(), request.getExplanation(), actor, now);
        persistComponentScore(leadId, "AI_CONFIDENCE", request.getAiConfidenceScore(), request.getExplanation(), actor, now);

        int overall = resolveOverallScore(request);
        String scoreType = StringUtils.hasText(request.getScoreType()) ? request.getScoreType() : "COMPOSITE";
        lead.applyScore(overall, actor, stateMachine);
        lead.applyConfidenceScore(request.getAiConfidenceScore(), actor);
        leadRepository.save(lead);
        scoreRepository.save(LeadScoreRecord.builder()
                .leadId(leadId)
                .score(overall)
                .scoreType(scoreType)
                .explanation(request.getExplanation())
                .scoredAt(now)
                .scoredBy(actor)
                .build());
        sideEffects.recordActivity(leadId, "SCORED",
                "Score " + overall + " (" + scoreType + ")", actor);
        eventPublisher.publish(LeadScoredEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                overall, scoreType, correlationId()));
        return leadMapper.toDto(lead);
    }

    private void persistComponentScore(UUID leadId, String type, Integer score, String explanation,
                                       UUID actor, Instant now) {
        if (score == null) {
            return;
        }
        scoreRepository.save(LeadScoreRecord.builder()
                .leadId(leadId)
                .score(score)
                .scoreType(type)
                .explanation(explanation)
                .scoredAt(now)
                .scoredBy(actor)
                .build());
    }

    private static int resolveOverallScore(ScoreLeadRequest request) {
        if (request.getScore() != null) {
            return request.getScore();
        }
        java.util.List<Integer> components = java.util.stream.Stream.of(
                        request.getBudgetScore(), request.getTimelineScore(), request.getLocationScore(),
                        request.getEngagementScore(), request.getAiConfidenceScore())
                .filter(java.util.Objects::nonNull)
                .toList();
        if (components.isEmpty()) {
            throw new ValidationException("Provide overall score or at least one component score");
        }
        return (int) Math.round(components.stream().mapToInt(Integer::intValue).average().orElseThrow());
    }

    @Transactional
    public LeadDto convertLead(UUID leadId, ConvertLeadRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        UUID customerId = customerConversionGateway.resolveCustomerId(lead, request.getCustomerId());
        lead.convert(customerId, actor, stateMachine);
        leadRepository.save(lead);
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), request.getReason(), actor);
        eventPublisher.publish(LeadConvertedEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                customerId.toString(),
                correlationId()));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadDto loseLead(UUID leadId, LoseLeadRequest request) {
        Lead lead = requireLead(leadId);
        UUID actor = actorId();
        LeadStatus old = lead.getStatus();
        lead.lose(actor, stateMachine);
        leadRepository.save(lead);
        sideEffects.recordStatusChange(leadId, old, lead.getStatus(), request.getReason(), actor);
        eventPublisher.publish(LeadLostEvent.of(
                lead.getTenantId().toString(), leadId.toString(), lead.getCustomerName(),
                request.getReason(), correlationId()));
        return leadMapper.toDto(lead);
    }

    @Transactional
    public LeadNoteDto addNote(UUID leadId, CreateLeadNoteRequest request) {
        requireLead(leadId);
        UUID actor = actorId();
        Instant now = Instant.now();
        LeadNote note = noteRepository.save(LeadNote.builder()
                .leadId(leadId)
                .note(request.getNote())
                .noteType(StringUtils.hasText(request.getNoteType()) ? request.getNoteType() : "GENERAL")
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .build());
        sideEffects.recordActivity(leadId, "NOTE_ADDED", "Note added", actor);
        return leadMapper.toNoteDto(note);
    }

    @Transactional(readOnly = true)
    public List<LeadNoteDto> listNotes(UUID leadId) {
        requireLead(leadId);
        return noteRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(leadMapper::toNoteDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadActivityDto> listActivities(UUID leadId) {
        requireLead(leadId);
        return activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(leadMapper::toActivityDto).toList();
    }

    @Transactional
    public LeadFollowupDto scheduleFollowup(UUID leadId, CreateLeadFollowupRequest request) {
        requireLead(leadId);
        UUID actor = actorId();
        Instant now = Instant.now();
        LeadFollowup followup = followupRepository.save(LeadFollowup.builder()
                .leadId(leadId)
                .scheduledAt(request.getScheduledAt())
                .followupType(request.getFollowupType())
                .note(request.getNote())
                .assignedTo(request.getAssignedTo())
                .createdBy(actor)
                .createdAt(now)
                .updatedAt(now)
                .build());
        sideEffects.recordActivity(leadId, "FOLLOWUP_SCHEDULED",
                "Follow-up " + request.getFollowupType() + " at " + request.getScheduledAt(), actor);
        return leadMapper.toFollowupDto(followup);
    }

    @Transactional(readOnly = true)
    public List<LeadFollowupDto> listFollowups(UUID leadId) {
        requireLead(leadId);
        return followupRepository.findByLeadIdOrderByScheduledAtAsc(leadId).stream()
                .map(leadMapper::toFollowupDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadStatusHistoryDto> listHistory(UUID leadId) {
        requireLead(leadId);
        return statusHistoryRepository.findByLeadIdOrderByCreatedAtDesc(leadId).stream()
                .map(leadMapper::toHistoryDto).toList();
    }

    @Transactional(readOnly = true)
    public List<LeadDuplicateDto> listDuplicates(Boolean resolved) {
        UUID tenantId = requireTenantId();
        boolean resolvedFlag = resolved != null && resolved;
        return duplicateRepository.findByTenantIdAndResolvedOrderByDetectedAtDesc(tenantId, resolvedFlag)
                .stream().map(leadMapper::toDuplicateDto).toList();
    }

    @Transactional
    public LeadDuplicateDto resolveDuplicate(UUID leadId, UUID duplicateId, UUID mergeIntoLeadId) {
        UUID tenantId = requireTenantId();
        requireLead(leadId);
        LeadDuplicate duplicate = duplicateRepository.findByTenantIdAndId(tenantId, duplicateId)
                .orElseThrow(() -> new NotFoundException("Duplicate not found: " + duplicateId));
        if (!duplicate.getLeadId().equals(leadId) && !duplicate.getDuplicateOfLeadId().equals(leadId)) {
            throw new ValidationException("Duplicate does not belong to lead");
        }
        duplicate.setResolved(true);
        duplicate.setMergedIntoLeadId(mergeIntoLeadId != null ? mergeIntoLeadId : leadId);
        duplicate.setMergedAt(Instant.now());
        return leadMapper.toDuplicateDto(duplicateRepository.save(duplicate));
    }

    private Lead requireLead(UUID leadId) {
        UUID tenantId = requireTenantId();
        return leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));
    }

    private PageResponse<LeadDto> toPage(Page<Lead> result) {
        return PageResponse.<LeadDto>builder()
                .content(result.getContent().stream().map(leadMapper::toDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .build();
    }

    private UUID requireTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new ValidationException("Tenant context is required");
        }
        try {
            return UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("Invalid tenant context");
        }
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
