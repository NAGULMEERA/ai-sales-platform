package com.aisales.workflow.application.service;

import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.workflow.domain.entity.WorkflowExecution;
import com.aisales.workflow.domain.enums.ConversationFollowupState;
import com.aisales.workflow.domain.enums.WorkflowDefinitionKey;
import com.aisales.workflow.infrastructure.persistence.WorkflowExecutionRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates conversation follow-up orchestration.
 * Message content and channel delivery remain in conversation-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationFollowupWorkflowService {

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void startOnConversationStarted(String tenantId, String conversationId, String correlationId) {
        UUID tenantUuid = UUID.fromString(tenantId);
        workflowExecutionRepository
                .findByTenantIdAndDefinitionKeyAndBusinessKey(
                        tenantUuid, WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1, conversationId)
                .ifPresentOrElse(
                        existing -> log.debug(
                                "Conversation follow-up workflow already exists for {}", conversationId),
                        () -> {
                            workflowExecutionRepository.save(WorkflowExecution.builder()
                                    .tenantId(tenantUuid)
                                    .definitionKey(WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1)
                                    .businessKey(conversationId)
                                    .state(ConversationFollowupState.OPEN.name())
                                    .correlationId(correlationId)
                                    .build());
                            log.info("Started conversation follow-up workflow tenant={} conversation={}",
                                    tenantId, conversationId);
                        });
    }

    @Transactional
    public void completeOnConversationClosed(String tenantId, String conversationId, String correlationId) {
        UUID tenantUuid = UUID.fromString(tenantId);
        WorkflowExecution execution = workflowExecutionRepository
                .findByTenantIdAndDefinitionKeyAndBusinessKey(
                        tenantUuid, WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1, conversationId)
                .orElseGet(() -> workflowExecutionRepository.save(WorkflowExecution.builder()
                        .tenantId(tenantUuid)
                        .definitionKey(WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1)
                        .businessKey(conversationId)
                        .state(ConversationFollowupState.OPEN.name())
                        .correlationId(correlationId)
                        .build()));

        if (ConversationFollowupState.COMPLETED.name().equals(execution.getState())) {
            log.debug("Conversation follow-up already completed for {}", conversationId);
            return;
        }

        execution.setState(ConversationFollowupState.COMPLETED.name());
        execution.setCompletedAt(Instant.now());
        if (execution.getCorrelationId() == null) {
            execution.setCorrelationId(correlationId);
        }
        WorkflowExecution saved = workflowExecutionRepository.save(execution);

        eventPublisher.publish(WorkflowCompletedEvent.of(
                tenantId,
                saved.getId().toString(),
                WorkflowDefinitionKey.CONVERSATION_FOLLOWUP_V1.name(),
                conversationId,
                correlationId != null ? correlationId : saved.getCorrelationId()));
        log.info("Completed conversation follow-up workflow tenant={} conversation={}",
                tenantId, conversationId);
    }
}
