package com.aisales.workflow.application.service;

import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.workflow.domain.entity.WorkflowExecution;
import com.aisales.workflow.domain.enums.OnboardingState;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingWorkflowServiceTest {

    @Mock private WorkflowExecutionRepository workflowExecutionRepository;
    @Mock private EventPublisher eventPublisher;
    @InjectMocks private OnboardingWorkflowService onboardingWorkflowService;

    @Test
    void shouldStartOnboardingOnUserCreated() {
        String tenantId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.ONBOARDING_V1, userId))
                .thenReturn(Optional.empty());
        when(workflowExecutionRepository.save(any(WorkflowExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        onboardingWorkflowService.startOnUserCreated(tenantId, userId, "corr-1");

        ArgumentCaptor<WorkflowExecution> captor = ArgumentCaptor.forClass(WorkflowExecution.class);
        verify(workflowExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getState())
                .isEqualTo(OnboardingState.AWAITING_EMAIL_VERIFICATION.name());
        assertThat(captor.getValue().getBusinessKey()).isEqualTo(userId);
    }

    @Test
    void shouldCompleteOnEmailVerifiedAndPublishEvent() {
        String tenantId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        UUID executionId = UUID.randomUUID();
        WorkflowExecution existing = WorkflowExecution.builder()
                .tenantId(UUID.fromString(tenantId))
                .definitionKey(WorkflowDefinitionKey.ONBOARDING_V1)
                .businessKey(userId)
                .state(OnboardingState.AWAITING_EMAIL_VERIFICATION.name())
                .build();
        existing.setId(executionId);

        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.ONBOARDING_V1, userId))
                .thenReturn(Optional.of(existing));
        when(workflowExecutionRepository.save(any(WorkflowExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        onboardingWorkflowService.completeOnEmailVerified(tenantId, userId, "corr-2");

        verify(workflowExecutionRepository).save(any(WorkflowExecution.class));
        ArgumentCaptor<WorkflowCompletedEvent> eventCaptor = ArgumentCaptor.forClass(WorkflowCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDefinitionKey()).isEqualTo("ONBOARDING_V1");
        assertThat(eventCaptor.getValue().getBusinessKey()).isEqualTo(userId);
        assertThat(existing.getState()).isEqualTo(OnboardingState.COMPLETED.name());
    }

    @Test
    void shouldIgnoreDuplicateCompletion() {
        String tenantId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        WorkflowExecution existing = WorkflowExecution.builder()
                .tenantId(UUID.fromString(tenantId))
                .definitionKey(WorkflowDefinitionKey.ONBOARDING_V1)
                .businessKey(userId)
                .state(OnboardingState.COMPLETED.name())
                .build();

        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.ONBOARDING_V1, userId))
                .thenReturn(Optional.of(existing));

        onboardingWorkflowService.completeOnEmailVerified(tenantId, userId, "corr-3");

        verify(workflowExecutionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
