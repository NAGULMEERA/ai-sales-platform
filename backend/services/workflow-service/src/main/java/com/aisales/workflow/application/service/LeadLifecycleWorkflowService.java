package com.aisales.workflow.application.service;

import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.workflow.domain.entity.WorkflowExecution;
import com.aisales.workflow.domain.enums.LeadLifecycleState;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates lead intake orchestration state.
 * Validation, qualification, and assignment decisions remain in lead-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadLifecycleWorkflowService {

    private static final Set<String> TERMINAL = Set.of(
            LeadLifecycleState.COMPLETED.name(),
            LeadLifecycleState.FAILED.name());

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void startOnLeadCreated(String tenantId, String leadId, String correlationId) {
        UUID tenantUuid = UUID.fromString(tenantId);
        workflowExecutionRepository
                .findByTenantIdAndDefinitionKeyAndBusinessKey(
                        tenantUuid, WorkflowDefinitionKey.LEAD_LIFECYCLE_V1, leadId)
                .ifPresentOrElse(
                        existing -> log.debug("Lead lifecycle workflow already exists for lead {}", leadId),
                        () -> {
                            workflowExecutionRepository.save(WorkflowExecution.builder()
                                    .tenantId(tenantUuid)
                                    .definitionKey(WorkflowDefinitionKey.LEAD_LIFECYCLE_V1)
                                    .businessKey(leadId)
                                    .state(LeadLifecycleState.AWAITING_VALIDATION.name())
                                    .correlationId(correlationId)
                                    .build());
                            log.info("Started lead lifecycle workflow tenant={} lead={}", tenantId, leadId);
                        });
    }

    @Transactional
    public void onLeadValidated(String tenantId, String leadId, String correlationId) {
        WorkflowExecution execution = requireOrStart(tenantId, leadId, correlationId);
        if (TERMINAL.contains(execution.getState())
                || LeadLifecycleState.VALIDATED.name().equals(execution.getState())
                || LeadLifecycleState.QUALIFIED.name().equals(execution.getState())
                || LeadLifecycleState.ASSIGNED.name().equals(execution.getState())) {
            return;
        }
        transition(execution, LeadLifecycleState.VALIDATED, correlationId);
        log.info("Lead lifecycle validated tenant={} lead={}", tenantId, leadId);
    }

    @Transactional
    public void onLeadQualified(String tenantId, String leadId, String correlationId) {
        WorkflowExecution execution = requireOrStart(tenantId, leadId, correlationId);
        if (TERMINAL.contains(execution.getState())
                || LeadLifecycleState.QUALIFIED.name().equals(execution.getState())
                || LeadLifecycleState.ASSIGNED.name().equals(execution.getState())) {
            return;
        }
        transition(execution, LeadLifecycleState.QUALIFIED, correlationId);
        log.info("Lead lifecycle qualified tenant={} lead={}", tenantId, leadId);
    }

    @Transactional
    public void completeOnLeadAssigned(String tenantId, String leadId, String correlationId) {
        WorkflowExecution execution = requireOrStart(tenantId, leadId, correlationId);
        if (LeadLifecycleState.COMPLETED.name().equals(execution.getState())) {
            log.debug("Lead lifecycle already completed for lead {}", leadId);
            return;
        }

        execution.setState(LeadLifecycleState.COMPLETED.name());
        execution.setCompletedAt(Instant.now());
        if (execution.getCorrelationId() == null) {
            execution.setCorrelationId(correlationId);
        }
        WorkflowExecution saved = workflowExecutionRepository.save(execution);

        eventPublisher.publish(WorkflowCompletedEvent.of(
                tenantId,
                saved.getId().toString(),
                WorkflowDefinitionKey.LEAD_LIFECYCLE_V1.name(),
                leadId,
                correlationId != null ? correlationId : saved.getCorrelationId()));
        log.info("Completed lead lifecycle workflow tenant={} lead={}", tenantId, leadId);
    }

    private WorkflowExecution requireOrStart(String tenantId, String leadId, String correlationId) {
        UUID tenantUuid = UUID.fromString(tenantId);
        return workflowExecutionRepository
                .findByTenantIdAndDefinitionKeyAndBusinessKey(
                        tenantUuid, WorkflowDefinitionKey.LEAD_LIFECYCLE_V1, leadId)
                .orElseGet(() -> workflowExecutionRepository.save(WorkflowExecution.builder()
                        .tenantId(tenantUuid)
                        .definitionKey(WorkflowDefinitionKey.LEAD_LIFECYCLE_V1)
                        .businessKey(leadId)
                        .state(LeadLifecycleState.AWAITING_VALIDATION.name())
                        .correlationId(correlationId)
                        .build()));
    }

    private void transition(WorkflowExecution execution, LeadLifecycleState target, String correlationId) {
        execution.setState(target.name());
        if (execution.getCorrelationId() == null) {
            execution.setCorrelationId(correlationId);
        }
        workflowExecutionRepository.save(execution);
    }
}
