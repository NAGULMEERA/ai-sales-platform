package com.aisales.workflow.infrastructure.messaging;

import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.ConversationClosedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.workflow.application.service.ConversationFollowupWorkflowService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class ConversationFollowupEventConsumer {

    static final String STARTED_CONSUMER = "workflow-conversation-started";
    static final String CLOSED_CONSUMER = "workflow-conversation-closed";

    private final IntegrationEventListener integrationEventListener;
    private final ConversationFollowupWorkflowService conversationFollowupWorkflowService;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "workflow-service-conversation-followup",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record,
                STARTED_CONSUMER,
                "ConversationStarted",
                ConversationStartedEvent.class,
                event -> conversationFollowupWorkflowService.startOnConversationStarted(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
        integrationEventListener.handleIfType(
                record,
                CLOSED_CONSUMER,
                "ConversationClosed",
                ConversationClosedEvent.class,
                event -> conversationFollowupWorkflowService.completeOnConversationClosed(
                        event.getTenantId(), event.getAggregateId(), event.getCorrelationId()));
    }
}
