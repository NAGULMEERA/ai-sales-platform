package com.aisales.common.events.outbox;

import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.common.exception.exception.EventPublishException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final PlatformMetrics platformMetrics;

    @Value("${aisales.events.default-topic:aisales-events}")
    private String defaultTopic;

    public OutboxEventPublisher(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.platformMetrics = null;
    }

    @Autowired
    public OutboxEventPublisher(OutboxRepository outboxRepository,
                                ObjectMapper objectMapper,
                                ObjectProvider<PlatformMetrics> platformMetrics) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.platformMetrics = platformMetrics != null ? platformMetrics.getIfAvailable() : null;
    }

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
            if (platformMetrics != null) {
                platformMetrics.incrementBusinessMetric(MetricNames.EVENT_PUBLISHED, event.getTenantId(),
                        "event_type", event.getEventType(), "topic", topic, "publisher", "outbox");
            }
        } catch (JsonProcessingException ex) {
            throw new EventPublishException("Failed to serialize outbox event: " + event.getEventType(), ex);
        }
    }

    private String resolveAggregateType(BaseEvent event) {
        return event.getClass().getSimpleName();
    }
}
