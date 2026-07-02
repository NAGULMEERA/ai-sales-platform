package com.aisales.common.events.outbox;

import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.EventPublishException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Persists integration events in the outbox table within the current business transaction.
 */
@Component
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${aisales.events.default-topic:aisales-events}")
    private String defaultTopic;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(BaseEvent event) {
        publish(defaultTopic, event);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String topic, BaseEvent event) {
        try {
            outboxRepository.save(OutboxEvent.builder()
                    .aggregateType(resolveAggregateType(event))
                    .aggregateId(event.getAggregateId())
                    .eventType(event.getEventType())
                    .payload(objectMapper.writeValueAsString(event))
                    .topic(topic)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .createdAt(Instant.now())
                    .retryCount(0)
                    .build());
        } catch (JsonProcessingException ex) {
            throw new EventPublishException("Failed to serialize outbox event: " + event.getEventType(), ex);
        }
    }

    private String resolveAggregateType(BaseEvent event) {
        return event.getClass().getSimpleName();
    }
}
