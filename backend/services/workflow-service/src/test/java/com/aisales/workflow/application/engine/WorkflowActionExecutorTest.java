package com.aisales.workflow.application.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.client.LeadServiceClient;
import com.aisales.common.contracts.lead.AssignLeadRequest;
import com.aisales.common.contracts.workflow.WorkflowActionDto;
import com.aisales.common.contracts.workflow.WorkflowActionType;
import com.aisales.common.events.model.NotificationSentEvent;
import com.aisales.common.events.model.WorkflowTriggeredEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class WorkflowActionExecutorTest {

    @Mock private EventPublisher eventPublisher;
    @Mock private ObjectProvider<LeadServiceClient> leadServiceClient;
    @Mock private ObjectProvider<com.aisales.common.contracts.client.DealServiceClient> dealServiceClient;
    @Mock private ObjectProvider<com.aisales.common.contracts.client.AiServiceClient> aiServiceClient;

    private WorkflowActionExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new WorkflowActionExecutor(
                eventPublisher, leadServiceClient, dealServiceClient, aiServiceClient);
    }

    @Test
    void shouldPublishNotificationEventForSendEmail() {
        String tenantId = UUID.randomUUID().toString();
        WorkflowActionDto action = WorkflowActionDto.builder()
                .type(WorkflowActionType.SEND_EMAIL)
                .params(Map.of("recipient", "ada@example.com", "template", "welcome"))
                .build();

        Map<String, Object> result = executor.execute(action, tenantId, "lead-1", Map.of());

        ArgumentCaptor<NotificationSentEvent> captor = ArgumentCaptor.forClass(NotificationSentEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo("EMAIL");
        assertThat(captor.getValue().getRecipient()).isEqualTo("ada@example.com");
        assertThat(result).containsEntry("channel", "EMAIL");
    }

    @Test
    void shouldPublishCoordinationEvent() {
        String tenantId = UUID.randomUUID().toString();
        WorkflowActionDto action = WorkflowActionDto.builder()
                .type(WorkflowActionType.PUBLISH_EVENT)
                .params(Map.of("ruleCode", "FOLLOWUP_V1"))
                .build();

        Map<String, Object> result = executor.execute(action, tenantId, "biz-1", Map.of());

        ArgumentCaptor<WorkflowTriggeredEvent> captor = ArgumentCaptor.forClass(WorkflowTriggeredEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getTriggerType()).isEqualTo("PUBLISH_EVENT");
        assertThat(result).containsEntry("published", true);
    }

    @Test
    void shouldAssignLeadViaClient() {
        UUID leadId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        LeadServiceClient client = mock(LeadServiceClient.class);
        when(leadServiceClient.getIfAvailable()).thenReturn(client);

        WorkflowActionDto action = WorkflowActionDto.builder()
                .type(WorkflowActionType.ASSIGN_LEAD)
                .params(Map.of("assignedTo", assignee.toString()))
                .build();

        Map<String, Object> result = executor.execute(
                action, UUID.randomUUID().toString(), leadId.toString(), Map.of("leadId", leadId.toString()));

        verify(client).assignLead(any(UUID.class), any(AssignLeadRequest.class));
        assertThat(result).containsEntry("leadId", leadId.toString());
    }

    @Test
    void shouldRejectMissingActionType() {
        assertThatThrownBy(() -> executor.execute(null, "t", "k", Map.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectAssignLeadWithoutIds() {
        WorkflowActionDto action = WorkflowActionDto.builder()
                .type(WorkflowActionType.ASSIGN_LEAD)
                .params(Map.of())
                .build();

        assertThatThrownBy(() -> executor.execute(action, "t", "k", Map.of()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("ASSIGN_LEAD");
    }
}
