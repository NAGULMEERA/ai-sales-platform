package com.aisales.analytics.infrastructure.messaging;

import com.aisales.analytics.application.service.AnalyticsRecordingService;
import com.aisales.analytics.domain.AnalyticsMetricNames;
import com.aisales.common.core.util.ObjectStrings;
import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.AiQualificationCompletedEvent;
import com.aisales.common.events.model.AiRecommendationGeneratedEvent;
import com.aisales.common.events.model.CatalogMatchedEvent;
import com.aisales.common.events.model.CatalogRecommendationGeneratedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.model.KnowledgeRetrievedEvent;
import com.aisales.common.events.model.LeadConvertedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import com.aisales.common.events.model.MessageReceivedEvent;
import com.aisales.common.events.model.MessageSentEvent;
import com.aisales.common.events.model.OpportunityCreatedEvent;
import com.aisales.common.events.model.OpportunityStatusChangedEvent;
import com.aisales.common.events.model.WorkflowCompletedEvent;
import com.aisales.common.events.model.WorkflowTriggeredEvent;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class AnalyticsEventConsumer {

    private final IntegrationEventListener integrationEventListener;
    private final AnalyticsRecordingService recordingService;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "analytics-service-facts",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record, "analytics-lead-created", LeadCreatedEvent.EVENT_TYPE, LeadCreatedEvent.class, event ->
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.LEAD_CREATED,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of(
                                        "source", nullSafe(event.getSource()),
                                        "status", nullSafe(event.getStatus()))));

        integrationEventListener.handleIfType(
                record, "analytics-lead-qualified", LeadQualifiedEvent.EVENT_TYPE, LeadQualifiedEvent.class, event ->
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.LEAD_QUALIFIED,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("status", nullSafe(event.getStatus()))));

        integrationEventListener.handleIfType(
                record, "analytics-lead-converted", LeadConvertedEvent.EVENT_TYPE, LeadConvertedEvent.class, event ->
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.LEAD_CONVERTED,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("customerId", nullSafe(event.getCustomerId()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-lead-status",
                LeadStatusChangedEvent.EVENT_TYPE, LeadStatusChangedEvent.class,
                event -> {
                    recordingService.recordCount(
                            event.getTenantId(),
                            AnalyticsMetricNames.LEAD_STATUS_CHANGED,
                            event.getOccurredAt(),
                            event.getCorrelationId(),
                            Map.of(
                                    "status",
                                    nullSafe(event.getNewStatus()),
                                    "previousStatus",
                                    nullSafe(event.getOldStatus())));
                    if ("LOST".equalsIgnoreCase(nullSafe(event.getNewStatus()))) {
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.LEAD_LOST,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("reason", nullSafe(event.getReason())));
                    }
                });

        integrationEventListener.handleIfType(
                record, "analytics-customer-created", CustomerCreatedEvent.EVENT_TYPE, CustomerCreatedEvent.class, event ->
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.CUSTOMER_CREATED,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of()));

        integrationEventListener.handleIfType(
                record,
                "analytics-opportunity-created",
                OpportunityCreatedEvent.EVENT_TYPE, OpportunityCreatedEvent.class,
                event -> recordingService.recordCount(
                        event.getTenantId(),
                        AnalyticsMetricNames.OPPORTUNITY_CREATED,
                        event.getOccurredAt(),
                        event.getCorrelationId(),
                        Map.of("status", nullSafe(event.getStatus()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-opportunity-status",
                OpportunityStatusChangedEvent.EVENT_TYPE, OpportunityStatusChangedEvent.class,
                event -> {
                    String status = nullSafe(event.getStatus()).toUpperCase(Locale.ROOT);
                    recordingService.recordCount(
                            event.getTenantId(),
                            AnalyticsMetricNames.OPPORTUNITY_STATUS_CHANGED,
                            event.getOccurredAt(),
                            event.getCorrelationId(),
                            Map.of(
                                    "status",
                                    status,
                                    "previousStatus",
                                    nullSafe(event.getPreviousStatus())));
                    if ("WON".equals(status) || "CLOSED_WON".equals(status)) {
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.OPPORTUNITY_WON,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("status", status));
                    } else if ("LOST".equals(status) || "CLOSED_LOST".equals(status)) {
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.OPPORTUNITY_LOST,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("status", status));
                    }
                });

        integrationEventListener.handleIfType(
                record,
                "analytics-conversation-started",
                ConversationStartedEvent.EVENT_TYPE, ConversationStartedEvent.class,
                event -> recordingService.recordCount(
                        event.getTenantId(),
                        AnalyticsMetricNames.CONVERSATION_STARTED,
                        event.getOccurredAt(),
                        event.getCorrelationId(),
                        Map.of()));

        integrationEventListener.handleIfType(
                record, "analytics-message-sent", MessageSentEvent.EVENT_TYPE, MessageSentEvent.class, event ->
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.MESSAGE_SENT,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of(
                                        "channel",
                                        nullSafe(event.getChannel()),
                                        "senderType",
                                        nullSafe(event.getSenderType()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-message-received",
                MessageReceivedEvent.EVENT_TYPE, MessageReceivedEvent.class,
                event -> recordingService.recordCount(
                        event.getTenantId(),
                        AnalyticsMetricNames.MESSAGE_RECEIVED,
                        event.getOccurredAt(),
                        event.getCorrelationId(),
                        Map.of("channel", nullSafe(event.getChannel()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-workflow-triggered",
                WorkflowTriggeredEvent.EVENT_TYPE, WorkflowTriggeredEvent.class,
                event -> recordingService.recordCount(
                        event.getTenantId(),
                        AnalyticsMetricNames.WORKFLOW_EXECUTED,
                        event.getOccurredAt(),
                        event.getCorrelationId(),
                        Map.of(
                                "definitionKey",
                                nullSafe(event.getDefinitionKey()),
                                "triggerType",
                                nullSafe(event.getTriggerType()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-workflow-completed",
                WorkflowCompletedEvent.EVENT_TYPE, WorkflowCompletedEvent.class,
                event -> recordingService.recordCount(
                        event.getTenantId(),
                        AnalyticsMetricNames.WORKFLOW_COMPLETED,
                        event.getOccurredAt(),
                        event.getCorrelationId(),
                        Map.of("definitionKey", nullSafe(event.getDefinitionKey()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-ai-qualification",
                AiQualificationCompletedEvent.EVENT_TYPE, AiQualificationCompletedEvent.class,
                event -> {
                    recordingService.recordCount(
                            event.getTenantId(),
                            AnalyticsMetricNames.AI_QUALIFICATION,
                            event.getOccurredAt(),
                            event.getCorrelationId(),
                            Map.of(
                                    "provider",
                                    nullSafe(event.getProvider()),
                                    "recommendation",
                                    nullSafe(event.getRecommendation())));
                    Double confidence = parseDouble(event.getConfidenceScore());
                    if (confidence != null) {
                        recordingService.record(
                                event.getTenantId(),
                                AnalyticsMetricNames.AI_QUALIFICATION_ACCURACY,
                                confidence,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("leadId", nullSafe(event.getLeadId())));
                    }
                });

        integrationEventListener.handleIfType(
                record,
                "analytics-ai-recommendation",
                AiRecommendationGeneratedEvent.EVENT_TYPE, AiRecommendationGeneratedEvent.class,
                event -> recordingService.recordCount(
                        event.getTenantId(),
                        AnalyticsMetricNames.AI_REQUEST,
                        event.getOccurredAt(),
                        event.getCorrelationId(),
                        Map.of("type", nullSafe(event.getRecommendationType()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-knowledge-retrieved",
                KnowledgeRetrievedEvent.EVENT_TYPE, KnowledgeRetrievedEvent.class,
                event -> recordingService.recordCount(
                        event.getTenantId(),
                        AnalyticsMetricNames.RAG_REQUEST,
                        event.getOccurredAt(),
                        event.getCorrelationId(),
                        Map.of("retriever", nullSafe(event.getRetriever()))));

        integrationEventListener.handleIfType(
                record, "analytics-catalog-matched", CatalogMatchedEvent.EVENT_TYPE, CatalogMatchedEvent.class, event ->
                        recordingService.recordCount(
                                event.getTenantId(),
                                AnalyticsMetricNames.CATALOG_MATCH,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("candidateCount", nullSafe(event.getCandidateCount()))));

        integrationEventListener.handleIfType(
                record,
                "analytics-catalog-recommendation",
                CatalogRecommendationGeneratedEvent.EVENT_TYPE, CatalogRecommendationGeneratedEvent.class,
                event -> {
                    recordingService.recordCount(
                            event.getTenantId(),
                            AnalyticsMetricNames.CATALOG_RECOMMENDATION,
                            event.getOccurredAt(),
                            event.getCorrelationId(),
                            Map.of(
                                    "topProductId",
                                    nullSafe(event.getTopProductId()),
                                    "leadId",
                                    nullSafe(event.getLeadId())));
                    Double confidence = parseDouble(event.getConfidence());
                    if (confidence != null) {
                        recordingService.record(
                                event.getTenantId(),
                                AnalyticsMetricNames.CATALOG_RECOMMENDATION_ACCURACY,
                                confidence,
                                event.getOccurredAt(),
                                event.getCorrelationId(),
                                Map.of("topProductId", nullSafe(event.getTopProductId())));
                    }
                });
    }

    private static String nullSafe(Object value) {
        return ObjectStrings.nullSafe(value);
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
