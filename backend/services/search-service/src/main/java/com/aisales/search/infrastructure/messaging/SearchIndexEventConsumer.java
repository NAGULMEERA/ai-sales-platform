package com.aisales.search.infrastructure.messaging;

import com.aisales.common.contracts.search.SearchEntityType;
import com.aisales.common.core.util.ObjectStrings;
import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.CatalogProductCreatedEvent;
import com.aisales.common.events.model.CatalogProductUpdatedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.common.events.model.CustomerCreatedEvent;
import com.aisales.common.events.model.CustomerUpdatedEvent;
import com.aisales.common.events.model.KnowledgeDocumentRegisteredEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.LeadStatusChangedEvent;
import com.aisales.common.events.model.OpportunityCreatedEvent;
import com.aisales.common.events.model.OpportunityStatusChangedEvent;
import com.aisales.search.application.service.SearchIndexingService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class SearchIndexEventConsumer {

    private final IntegrationEventListener integrationEventListener;
    private final SearchIndexingService indexingService;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "search-service-indexer",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record, "search-lead-created", LeadCreatedEvent.EVENT_TYPE, LeadCreatedEvent.class, event ->
                        index(
                                event.getTenantId(),
                                SearchEntityType.LEAD,
                                event.getAggregateId(),
                                event.getLeadName(),
                                "Lead " + nullSafe(event.getLeadName()) + " source=" + nullSafe(event.getSource()),
                                event.getSource(),
                                event.getStatus(),
                                event.getSource(),
                                0d,
                                Map.of("status", nullSafe(event.getStatus()))));

        integrationEventListener.handleIfType(
                record, "search-lead-qualified", LeadQualifiedEvent.EVENT_TYPE, LeadQualifiedEvent.class, event ->
                        index(
                                event.getTenantId(),
                                SearchEntityType.LEAD,
                                event.getAggregateId(),
                                event.getLeadName(),
                                "Qualified lead score=" + event.getScore(),
                                null,
                                event.getStatus(),
                                null,
                                event.getScore() == null ? 80d : event.getScore().doubleValue(),
                                Map.of("qualified", true)));

        integrationEventListener.handleIfType(
                record,
                "search-lead-status",
                LeadStatusChangedEvent.EVENT_TYPE, LeadStatusChangedEvent.class,
                event -> index(
                        event.getTenantId(),
                        SearchEntityType.LEAD,
                        event.getAggregateId(),
                        event.getLeadName(),
                        "Status " + event.getOldStatus() + " -> " + event.getNewStatus(),
                        null,
                        event.getNewStatus(),
                        null,
                        null,
                        Map.of("status", nullSafe(event.getNewStatus()))));

        integrationEventListener.handleIfType(
                record, "search-customer-created", CustomerCreatedEvent.EVENT_TYPE, CustomerCreatedEvent.class, event ->
                        index(
                                event.getTenantId(),
                                SearchEntityType.CUSTOMER,
                                event.getAggregateId(),
                                event.getCustomerName(),
                                "Customer " + nullSafe(event.getEmail()),
                                event.getEmail(),
                                "ACTIVE",
                                null,
                                50d,
                                Map.of("email", nullSafe(event.getEmail()))));

        integrationEventListener.handleIfType(
                record, "search-customer-updated", CustomerUpdatedEvent.EVENT_TYPE, CustomerUpdatedEvent.class, event ->
                        index(
                                event.getTenantId(),
                                SearchEntityType.CUSTOMER,
                                event.getAggregateId(),
                                event.getCustomerName(),
                                "Customer status=" + event.getStatus(),
                                null,
                                event.getStatus(),
                                null,
                                null,
                                Map.of("status", nullSafe(event.getStatus()))));

        integrationEventListener.handleIfType(
                record,
                "search-catalog-created",
                CatalogProductCreatedEvent.EVENT_TYPE, CatalogProductCreatedEvent.class,
                event -> index(
                        event.getTenantId(),
                        SearchEntityType.CATALOG,
                        event.getAggregateId(),
                        event.getName(),
                        "Code " + nullSafe(event.getCode()) + " type=" + nullSafe(event.getProductType()),
                        event.getCode(),
                        event.getStatus(),
                        event.getProductType(),
                        40d,
                        Map.of("code", nullSafe(event.getCode()), "productType", nullSafe(event.getProductType()))));

        integrationEventListener.handleIfType(
                record,
                "search-catalog-updated",
                CatalogProductUpdatedEvent.EVENT_TYPE, CatalogProductUpdatedEvent.class,
                event -> index(
                        event.getTenantId(),
                        SearchEntityType.CATALOG,
                        event.getAggregateId(),
                        event.getName(),
                        "Code " + nullSafe(event.getCode()),
                        event.getCode(),
                        event.getStatus(),
                        event.getProductType(),
                        40d,
                        Map.of("code", nullSafe(event.getCode()))));

        integrationEventListener.handleIfType(
                record,
                "search-opportunity-created",
                OpportunityCreatedEvent.EVENT_TYPE, OpportunityCreatedEvent.class,
                event -> index(
                        event.getTenantId(),
                        SearchEntityType.OPPORTUNITY,
                        event.getAggregateId(),
                        event.getName(),
                        "Opportunity for customer " + nullSafe(event.getCustomerId()),
                        null,
                        event.getStatus(),
                        null,
                        60d,
                        Map.of(
                                "customerId",
                                nullSafe(event.getCustomerId()),
                                "leadId",
                                nullSafe(event.getLeadId()))));

        integrationEventListener.handleIfType(
                record,
                "search-opportunity-status",
                OpportunityStatusChangedEvent.EVENT_TYPE, OpportunityStatusChangedEvent.class,
                event -> index(
                        event.getTenantId(),
                        SearchEntityType.OPPORTUNITY,
                        event.getAggregateId(),
                        "Opportunity " + event.getAggregateId(),
                        "Status " + event.getPreviousStatus() + " -> " + event.getStatus(),
                        null,
                        event.getStatus(),
                        null,
                        null,
                        Map.of("status", nullSafe(event.getStatus()))));

        integrationEventListener.handleIfType(
                record,
                "search-conversation-started",
                ConversationStartedEvent.EVENT_TYPE, ConversationStartedEvent.class,
                event -> index(
                        event.getTenantId(),
                        SearchEntityType.CONVERSATION,
                        event.getAggregateId(),
                        "Conversation " + nullSafe(event.getChannel()),
                        "Lead " + nullSafe(event.getLeadId()) + " channel=" + nullSafe(event.getChannel()),
                        null,
                        event.getStatus(),
                        event.getChannel(),
                        30d,
                        Map.of(
                                "channel",
                                nullSafe(event.getChannel()),
                                "leadId",
                                nullSafe(event.getLeadId()))));

        integrationEventListener.handleIfType(
                record,
                "search-knowledge-registered",
                KnowledgeDocumentRegisteredEvent.EVENT_TYPE, KnowledgeDocumentRegisteredEvent.class,
                event -> index(
                        event.getTenantId(),
                        SearchEntityType.KNOWLEDGE,
                        event.getAggregateId(),
                        event.getName(),
                        "Knowledge document kb=" + nullSafe(event.getKnowledgeBaseId()),
                        event.getName(),
                        event.getStatus(),
                        "KNOWLEDGE",
                        20d,
                        Map.of(
                                "knowledgeBaseId",
                                nullSafe(event.getKnowledgeBaseId()),
                                "mediaId",
                                nullSafe(event.getMediaId()))));
    }

    private void index(
            String tenantId,
            SearchEntityType type,
            String entityId,
            String title,
            String body,
            String keywords,
            String status,
            String source,
            Double businessScore,
            Map<String, Object> metadata) {
        indexingService.upsert(
                UUID.fromString(tenantId),
                null,
                type,
                UUID.fromString(entityId),
                title,
                body,
                keywords,
                status,
                source,
                businessScore,
                0L,
                Instant.now(),
                metadata == null ? new HashMap<>() : new HashMap<>(metadata));
    }

    private static String nullSafe(Object value) {
        return ObjectStrings.nullSafe(value);
    }
}
