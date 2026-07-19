package com.aisales.workflow.infrastructure.messaging;

import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.LeadAssignedEvent;
import com.aisales.common.events.model.LeadCreatedEvent;
import com.aisales.common.events.model.LeadQualifiedEvent;
import com.aisales.common.events.model.LeadValidatedEvent;
import com.aisales.workflow.application.service.LeadLifecycleWorkflowService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class LeadLifecycleEventConsumer {

    static final String LEAD_CREATED_CONSUMER = "workflow-lead-lifecycle-created";
    static final String LEAD_VALIDATED_CONSUMER = "workflow-lead-lifecycle-validated";
    static final String LEAD_QUALIFIED_CONSUMER = "workflow-lead-lifecycle-qualified";
    static final String LEAD_ASSIGNED_CONSUMER = "workflow-lead-lifecycle-assigned";

    private final IntegrationEventListener integrationEventListener;
    private final LeadLifecycleWorkflowService leadLifecycleWorkflowService;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "workflow-service-lead-lifecycle",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record,
                LEAD_CREATED_CONSUMER,
                LeadCreatedEvent.EVENT_TYPE, LeadCreatedEvent.class,
                event -> leadLifecycleWorkflowService.startOnLeadCreated(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
        integrationEventListener.handleIfType(
                record,
                LEAD_VALIDATED_CONSUMER,
                LeadValidatedEvent.EVENT_TYPE, LeadValidatedEvent.class,
                event -> leadLifecycleWorkflowService.onLeadValidated(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
        integrationEventListener.handleIfType(
                record,
                LEAD_QUALIFIED_CONSUMER,
                LeadQualifiedEvent.EVENT_TYPE, LeadQualifiedEvent.class,
                event -> leadLifecycleWorkflowService.onLeadQualified(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
        integrationEventListener.handleIfType(
                record,
                LEAD_ASSIGNED_CONSUMER,
                LeadAssignedEvent.EVENT_TYPE, LeadAssignedEvent.class,
                event -> leadLifecycleWorkflowService.completeOnLeadAssigned(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
    }
}
