package com.aisales.workflow.infrastructure.messaging;

import com.aisales.common.contracts.workflow.WorkflowTriggerType;
import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.AiQualificationCompletedEvent;
import com.aisales.common.events.model.CatalogRecommendationGeneratedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.common.events.model.LeadConvertedToCustomerEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.MessageReceivedEvent;
import com.aisales.common.events.model.MessageSentEvent;
import com.aisales.common.events.model.OpportunityCreatedEvent;
import com.aisales.workflow.application.engine.WorkflowAutomationEngine;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class WorkflowAutomationEventConsumer {

    private final IntegrationEventListener integrationEventListener;
    private final WorkflowAutomationEngine automationEngine;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "workflow-service-automation",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record,
                "workflow-automation-lead-created",
                "LeadCreated",
                LeadCreatedEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.LEAD_CREATED,
                        event.getAggregateId(),
                        Map.of("leadId", event.getAggregateId()),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-lead-qualified",
                "LeadQualified",
                LeadQualifiedEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.LEAD_QUALIFIED,
                        event.getAggregateId(),
                        context("leadId", event.getAggregateId(), "leadScore", event.getScore()),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-opportunity-created",
                "OpportunityCreated",
                OpportunityCreatedEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.OPPORTUNITY_CREATED,
                        event.getAggregateId(),
                        context(
                                "opportunityId",
                                event.getAggregateId(),
                                "leadId",
                                event.getLeadId(),
                                "customerId",
                                event.getCustomerId(),
                                "opportunityStage",
                                event.getStatus()),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-conversation-started",
                "ConversationStarted",
                ConversationStartedEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.CONVERSATION_STARTED,
                        event.getAggregateId(),
                        context(
                                "conversationId",
                                event.getAggregateId(),
                                "leadId",
                                event.getLeadId(),
                                "customerId",
                                event.getCustomerId()),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-message-received",
                "MessageReceived",
                MessageReceivedEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.MESSAGE_RECEIVED,
                        event.getConversationId(),
                        context(
                                "conversationId",
                                event.getConversationId(),
                                "leadId",
                                event.getLeadId(),
                                "messageId",
                                event.getMessageId()),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-message-sent",
                "MessageSent",
                MessageSentEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.MESSAGE_SENT,
                        event.getConversationId(),
                        context(
                                "conversationId",
                                event.getConversationId(),
                                "leadId",
                                event.getLeadId(),
                                "messageId",
                                event.getMessageId()),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-customer-converted",
                "LeadConvertedToCustomer",
                LeadConvertedToCustomerEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.CUSTOMER_CONVERTED,
                        event.getAggregateId(),
                        context(
                                "customerId",
                                event.getAggregateId(),
                                "leadId",
                                event.getLeadId(),
                                "customerExists",
                                true),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-ai-qualification",
                "AIQualificationCompleted",
                AiQualificationCompletedEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.AI_QUALIFICATION_COMPLETED,
                        event.getLeadId() != null ? event.getLeadId() : event.getAggregateId(),
                        context(
                                "leadId",
                                event.getLeadId(),
                                "aiConfidence",
                                event.getConfidenceScore(),
                                "leadScore",
                                event.getQualificationScore()),
                        event.getCorrelationId()));

        integrationEventListener.handleIfType(
                record,
                "workflow-automation-catalog-recommendation",
                "CatalogRecommendationGenerated",
                CatalogRecommendationGeneratedEvent.class,
                event -> automationEngine.onTrigger(
                        event.getTenantId(),
                        WorkflowTriggerType.CATALOG_RECOMMENDATION_GENERATED,
                        event.getAggregateId(),
                        context(
                                "leadId",
                                event.getLeadId(),
                                "catalogAvailable",
                                true,
                                "productId",
                                event.getTopProductId()),
                        event.getCorrelationId()));
    }

    private static Map<String, Object> context(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null && value != null) {
                map.put(String.valueOf(key), value);
            }
        }
        return map;
    }
}
