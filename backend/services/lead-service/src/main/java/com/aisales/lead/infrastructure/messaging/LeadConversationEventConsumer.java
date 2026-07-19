package com.aisales.lead.infrastructure.messaging;

import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.ConversationClosedEvent;
import com.aisales.common.events.model.ConversationMessageAddedEvent;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.lead.application.service.LeadConversationTimelineService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
public class LeadConversationEventConsumer {

    static final String STARTED_CONSUMER = "lead-conversation-started";
    static final String MESSAGE_CONSUMER = "lead-conversation-message";
    static final String CLOSED_CONSUMER = "lead-conversation-closed";

    private final IntegrationEventListener integrationEventListener;
    private final LeadConversationTimelineService timelineService;

    @KafkaListener(
            topics = "${aisales.events.default-topic:aisales-events}",
            groupId = "lead-service-conversation-timeline",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handleIfType(
                record,
                STARTED_CONSUMER,
                ConversationStartedEvent.EVENT_TYPE, ConversationStartedEvent.class,
                event -> timelineService.onConversationStarted(
                        event.getLeadId(), event.getAggregateId(), event.getChannel()));
        integrationEventListener.handleIfType(
                record,
                MESSAGE_CONSUMER,
                ConversationMessageAddedEvent.EVENT_TYPE, ConversationMessageAddedEvent.class,
                event -> timelineService.onMessageAdded(
                        event.getLeadId(),
                        event.getConversationId(),
                        event.getMessageId(),
                        event.getSenderType()));
        integrationEventListener.handleIfType(
                record,
                CLOSED_CONSUMER,
                ConversationClosedEvent.EVENT_TYPE, ConversationClosedEvent.class,
                event -> timelineService.onConversationClosed(
                        event.getLeadId(), event.getAggregateId(), event.getReason()));
    }
}
