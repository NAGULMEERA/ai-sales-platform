package com.aisales.workflow.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.workflow.domain.entity.WorkflowExecution;
import com.aisales.workflow.domain.enums.LeadLifecycleState;
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

@ExtendWith(MockitoExtension.class)
class LeadLifecycleWorkflowServiceTest {

    @Mock private WorkflowExecutionRepository workflowExecutionRepository;
    @Mock private EventPublisher eventPublisher;
    @InjectMocks private LeadLifecycleWorkflowService leadLifecycleWorkflowService;

    @Test
    void shouldStartOnLeadCreated() {
        String tenantId = UUID.randomUUID().toString();
        String leadId = UUID.randomUUID().toString();
        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.LEAD_LIFECYCLE_V1, leadId))
                .thenReturn(Optional.empty());
        when(workflowExecutionRepository.save(any(WorkflowExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        leadLifecycleWorkflowService.startOnLeadCreated(tenantId, leadId, "corr-1");

        ArgumentCaptor<WorkflowExecution> captor = ArgumentCaptor.forClass(WorkflowExecution.class);
        verify(workflowExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getState())
                .isEqualTo(LeadLifecycleState.AWAITING_VALIDATION.name());
        assertThat(captor.getValue().getDefinitionKey())
                .isEqualTo(WorkflowDefinitionKey.LEAD_LIFECYCLE_V1);
    }

    @Test
    void shouldTransitionOnValidatedAndCompleteOnAssigned() {
        String tenantId = UUID.randomUUID().toString();
        String leadId = UUID.randomUUID().toString();
        UUID executionId = UUID.randomUUID();
        WorkflowExecution existing = WorkflowExecution.builder()
                .tenantId(UUID.fromString(tenantId))
                .definitionKey(WorkflowDefinitionKey.LEAD_LIFECYCLE_V1)
                .businessKey(leadId)
                .state(LeadLifecycleState.AWAITING_VALIDATION.name())
                .build();
        existing.setId(executionId);

        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.LEAD_LIFECYCLE_V1, leadId))
                .thenReturn(Optional.of(existing));
        when(workflowExecutionRepository.save(any(WorkflowExecution.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        leadLifecycleWorkflowService.onLeadValidated(tenantId, leadId, "corr-2");
        assertThat(existing.getState()).isEqualTo(LeadLifecycleState.VALIDATED.name());

        leadLifecycleWorkflowService.onLeadQualified(tenantId, leadId, "corr-3");
        assertThat(existing.getState()).isEqualTo(LeadLifecycleState.QUALIFIED.name());

        leadLifecycleWorkflowService.completeOnLeadAssigned(tenantId, leadId, "corr-4");
        assertThat(existing.getState()).isEqualTo(LeadLifecycleState.COMPLETED.name());

        ArgumentCaptor<WorkflowCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDefinitionKey()).isEqualTo("LEAD_LIFECYCLE_V1");
        assertThat(eventCaptor.getValue().getBusinessKey()).isEqualTo(leadId);
    }

    @Test
    void shouldIgnoreDuplicateCompletion() {
        String tenantId = UUID.randomUUID().toString();
        String leadId = UUID.randomUUID().toString();
        WorkflowExecution existing = WorkflowExecution.builder()
                .tenantId(UUID.fromString(tenantId))
                .definitionKey(WorkflowDefinitionKey.LEAD_LIFECYCLE_V1)
                .businessKey(leadId)
                .state(LeadLifecycleState.COMPLETED.name())
                .build();

        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.LEAD_LIFECYCLE_V1, leadId))
                .thenReturn(Optional.of(existing));

        leadLifecycleWorkflowService.completeOnLeadAssigned(tenantId, leadId, "corr-5");

        verify(workflowExecutionRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }
}
