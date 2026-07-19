package com.aisales.workflow.application.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.workflow.WorkflowActionDto;
import com.aisales.common.contracts.workflow.WorkflowActionType;
import com.aisales.common.contracts.workflow.WorkflowConditionDto;
import com.aisales.common.contracts.workflow.WorkflowConditionType;
import com.aisales.common.contracts.workflow.WorkflowTriggerType;
import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.model.WorkflowTriggeredEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.workflow.domain.entity.WorkflowAutomationExecution;
import com.aisales.workflow.domain.entity.WorkflowExecutionHistory;
import com.aisales.workflow.domain.entity.WorkflowRule;
import com.aisales.workflow.infrastructure.persistence.WorkflowAutomationExecutionRepository;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionHistoryRepository;
import com.aisales.workflow.infrastructure.persistence.WorkflowRuleRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class WorkflowAutomationEngineTest {

    @Mock private WorkflowRuleRepository ruleRepository;
    @Mock private WorkflowAutomationExecutionRepository executionRepository;
    @Mock private WorkflowExecutionHistoryRepository historyRepository;
    @Mock private WorkflowActionExecutor actionExecutor;
    @Mock private EventPublisher eventPublisher;
    @Mock private ObjectProvider<com.aisales.common.observability.metrics.PlatformMetrics> platformMetrics;
    @Mock private TransactionTemplate transactionTemplate;

    private WorkflowAutomationEngine engine;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        doAnswer(invocation -> {
                    TransactionCallback<?> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                })
                .when(transactionTemplate)
                .execute(any());
        doAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<TransactionStatus> action = invocation.getArgument(0);
                    action.accept(mock(TransactionStatus.class));
                    return null;
                })
                .when(transactionTemplate)
                .executeWithoutResult(any());
        engine = new WorkflowAutomationEngine(
                ruleRepository,
                executionRepository,
                historyRepository,
                new WorkflowConditionEvaluator(),
                actionExecutor,
                eventPublisher,
                platformMetrics,
                transactionTemplate);
    }

    @Test
    void shouldExecuteMatchingRuleAndPublishEvents() {
        WorkflowRule rule = WorkflowRule.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .code("FOLLOWUP_ON_MESSAGE")
                .name("Follow-up")
                .triggerType(WorkflowTriggerType.MESSAGE_RECEIVED)
                .conditions(List.of(WorkflowConditionDto.builder()
                        .type(WorkflowConditionType.ALWAYS)
                        .build()))
                .actions(List.of(WorkflowActionDto.builder()
                        .type(WorkflowActionType.SEND_NOTIFICATION)
                        .params(Map.of("channel", "EMAIL", "recipient", "a@b.com"))
                        .build()))
                .maxRetries(2)
                .retryBackoffSeconds(10)
                .enabled(true)
                .build();

        Map<UUID, WorkflowAutomationExecution> executions = new HashMap<>();
        when(ruleRepository.findByTenantIdAndTriggerTypeAndEnabledTrueAndDeletedAtIsNull(
                        tenantId, WorkflowTriggerType.MESSAGE_RECEIVED))
                .thenReturn(List.of(rule));
        when(executionRepository.save(any(WorkflowAutomationExecution.class))).thenAnswer(inv -> {
            WorkflowAutomationExecution e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(UUID.randomUUID());
            }
            executions.put(e.getId(), e);
            return e;
        });
        when(executionRepository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(executions.get(inv.getArgument(0))));
        when(historyRepository.save(any(WorkflowExecutionHistory.class))).thenAnswer(inv -> inv.getArgument(0));
        when(actionExecutor.execute(any(), eq(tenantId.toString()), eq("conv-1"), any()))
                .thenReturn(Map.of("sent", true));

        engine.onTrigger(
                tenantId.toString(),
                WorkflowTriggerType.MESSAGE_RECEIVED,
                "conv-1",
                Map.of("conversationId", "conv-1"),
                "corr-1");

        ArgumentCaptor<BaseEvent> published = ArgumentCaptor.forClass(BaseEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeast(2)).publish(published.capture());
        assertThat(published.getAllValues())
                .anyMatch(e -> e instanceof WorkflowTriggeredEvent)
                .anyMatch(e -> e instanceof WorkflowCompletedEvent
                        && "conv-1".equals(((WorkflowCompletedEvent) e).getBusinessKey()));
    }

    @Test
    void shouldSkipActionsWhenConditionsFail() {
        WorkflowRule rule = WorkflowRule.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .code("HIGH_SCORE_ONLY")
                .name("High score")
                .triggerType(WorkflowTriggerType.LEAD_QUALIFIED)
                .conditions(List.of(WorkflowConditionDto.builder()
                        .type(WorkflowConditionType.LEAD_SCORE_GT)
                        .params(Map.of("value", 90))
                        .build()))
                .actions(List.of(WorkflowActionDto.builder()
                        .type(WorkflowActionType.PUBLISH_EVENT)
                        .build()))
                .maxRetries(1)
                .retryBackoffSeconds(5)
                .enabled(true)
                .build();

        Map<UUID, WorkflowAutomationExecution> executions = new HashMap<>();
        when(ruleRepository.findByTenantIdAndTriggerTypeAndEnabledTrueAndDeletedAtIsNull(
                        tenantId, WorkflowTriggerType.LEAD_QUALIFIED))
                .thenReturn(List.of(rule));
        when(executionRepository.save(any(WorkflowAutomationExecution.class))).thenAnswer(inv -> {
            WorkflowAutomationExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            executions.put(e.getId(), e);
            return e;
        });
        when(executionRepository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(executions.get(inv.getArgument(0))));
        when(historyRepository.save(any(WorkflowExecutionHistory.class))).thenAnswer(inv -> inv.getArgument(0));

        engine.onTrigger(
                tenantId.toString(),
                WorkflowTriggerType.LEAD_QUALIFIED,
                "lead-1",
                Map.of("leadScore", 50),
                "corr-2");

        verify(actionExecutor, org.mockito.Mockito.never()).execute(any(), any(), any(), any());
    }
}
