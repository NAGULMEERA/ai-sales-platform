package com.company.platform.template.messaging;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Production-ready Kafka Producer Template.
 *
 * Responsibilities
 * - Publish integration events
 * - Preserve correlation and tenant context
 * - Send immutable event envelope
 * - Remain compatible with Outbox Pattern
 */
@Component
public class KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    public KafkaEventPublisher(KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String topic, String key, EventEnvelope<?> event) {

        kafkaTemplate.send(topic, key, event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event {} correlationId={}",
                            event.eventType(), event.correlationId(), ex);
                    return;
                }

                log.info("Published event={} topic={} partition={} offset={} correlationId={}",
                        event.eventType(),
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        event.correlationId());
            });
    }
}

/**
 * Immutable event envelope.
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID tenantId,
        UUID aggregateId,
        UUID correlationId,
        Instant occurredAt,
        T payload) {

    public static <T> EventEnvelope<T> of(
            String eventType,
            UUID tenantId,
            UUID aggregateId,
            UUID correlationId,
            T payload) {

        return new EventEnvelope<>(
                UUID.randomUUID(),
                eventType,
                1,
                tenantId,
                aggregateId,
                correlationId,
                Instant.now(),
                payload);
    }
}

/**
 * Example payload.
 */
record LeadQualifiedEvent(
        UUID leadId,
        int score) {}
