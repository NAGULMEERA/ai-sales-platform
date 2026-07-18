package com.aisales.workflow.application.service;

import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.workflow.domain.entity.WorkflowExecution;
import com.aisales.workflow.domain.enums.OnboardingState;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates tenant-user onboarding execution state.
 * Business decisions (verification, roles, trial) remain in owning services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingWorkflowService {

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void startOnUserCreated(String tenantId, String userId, String correlationId) {
        UUID tenantUuid = UUID.fromString(tenantId);
        workflowExecutionRepository
                .findByTenantIdAndDefinitionKeyAndBusinessKey(
                        tenantUuid, WorkflowDefinitionKey.ONBOARDING_V1, userId)
                .ifPresentOrElse(
                        existing -> log.debug("Onboarding workflow already exists for user {}", userId),
                        () -> {
                            WorkflowExecution execution = WorkflowExecution.builder()
                                    .tenantId(tenantUuid)
                                    .definitionKey(WorkflowDefinitionKey.ONBOARDING_V1)
                                    .businessKey(userId)
                                    .state(OnboardingState.AWAITING_EMAIL_VERIFICATION.name())
                                    .correlationId(correlationId)
                                    .build();
                            workflowExecutionRepository.save(execution);
                            log.info("Started onboarding workflow tenant={} user={}", tenantId, userId);
                        });
    }

    @Transactional
    public void completeOnEmailVerified(String tenantId, String userId, String correlationId) {
        UUID tenantUuid = UUID.fromString(tenantId);
        WorkflowExecution execution = workflowExecutionRepository
                .findByTenantIdAndDefinitionKeyAndBusinessKey(
                        tenantUuid, WorkflowDefinitionKey.ONBOARDING_V1, userId)
                .orElseGet(() -> WorkflowExecution.builder()
                        .tenantId(tenantUuid)
                        .definitionKey(WorkflowDefinitionKey.ONBOARDING_V1)
                        .businessKey(userId)
                        .state(OnboardingState.AWAITING_EMAIL_VERIFICATION.name())
                        .correlationId(correlationId)
                        .build());

        if (OnboardingState.COMPLETED.name().equals(execution.getState())) {
            log.debug("Onboarding already completed for user {}", userId);
            return;
        }
        if (!OnboardingState.AWAITING_EMAIL_VERIFICATION.name().equals(execution.getState())
                && !OnboardingState.CREATED.name().equals(execution.getState())) {
            throw new IllegalStateException(
                    "Invalid onboarding transition from " + execution.getState() + " to COMPLETED");
        }

        execution.setState(OnboardingState.COMPLETED.name());
        execution.setCompletedAt(Instant.now());
        if (execution.getCorrelationId() == null) {
            execution.setCorrelationId(correlationId);
        }
        WorkflowExecution saved = workflowExecutionRepository.save(execution);

        eventPublisher.publish(WorkflowCompletedEvent.of(
                tenantId,
                saved.getId().toString(),
                WorkflowDefinitionKey.ONBOARDING_V1.name(),
                userId,
                correlationId != null ? correlationId : saved.getCorrelationId()));
        log.info("Completed onboarding workflow tenant={} user={}", tenantId, userId);
    }
}
