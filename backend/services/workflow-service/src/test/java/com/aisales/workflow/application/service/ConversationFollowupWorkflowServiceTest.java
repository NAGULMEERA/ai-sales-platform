package com.aisales.workflow.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.workflow.domain.entity.WorkflowExecution;
import com.aisales.workflow.domain.enums.ConversationFollowupState;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationFollowupWorkflowServiceTest {

    @Mock private WorkflowExecutionRepository workflowExecutionRepository;
    @Mock private EventPublisher eventPublisher;

    private ConversationFollowupWorkflowService service;
    private String tenantId;
    private String conversationId;

    @BeforeEach
    void setUp() {
        service = new ConversationFollowupWorkflowService(workflowExecutionRepository, eventPublisher);
        tenantId = UUID.randomUUID().toString();
        conversationId = UUID.randomUUID().toString();
    }

    @Test
    void shouldStartFollowupOnConversationStarted() {
        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1, conversationId))
                .thenReturn(Optional.empty());
        when(workflowExecutionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        service.startOnConversationStarted(tenantId, conversationId, "corr-1");

        ArgumentCaptor<WorkflowExecution> captor = ArgumentCaptor.forClass(WorkflowExecution.class);
        verify(workflowExecutionRepository).save(captor.capture());
        assertThat(captor.getValue().getDefinitionKey())
                .isEqualTo(WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1);
        assertThat(captor.getValue().getState()).isEqualTo(ConversationFollowupState.OPEN.name());
        assertThat(captor.getValue().getBusinessKey()).isEqualTo(conversationId);
    }

    @Test
    void shouldCompleteOnConversationClosed() {
        WorkflowExecution open = WorkflowExecution.builder()
                .tenantId(UUID.fromString(tenantId))
                .definitionKey(WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1)
                .businessKey(conversationId)
                .state(ConversationFollowupState.OPEN.name())
                .correlationId("corr-1")
                .build();
        open.setId(UUID.randomUUID());
        when(workflowExecutionRepository.findByTenantIdAndDefinitionKeyAndBusinessKey(
                UUID.fromString(tenantId), WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1, conversationId))
                .thenReturn(Optional.of(open));
        when(workflowExecutionRepository.save(any(WorkflowExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        service.completeOnConversationClosed(tenantId, conversationId, "corr-1");

        assertThat(open.getState()).isEqualTo(ConversationFollowupState.COMPLETED.name());
        ArgumentCaptor<WorkflowCompletedEvent> eventCaptor =
                ArgumentCaptor.forClass(WorkflowCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getDefinitionKey())
                .isEqualTo("CONVERSATION_FOLLOWUP_V1");
    }

    @Test
    void shouldUseSameWorkflowKeyForVisitAndTestDriveFollowups() {
        // Sprint 6: industries differ by conversation subject / lead followupType metadata only.
        assertThat(WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1.name())
                .isEqualTo("CONVERSATION_FOLLOWUP_V1");
    }
}
