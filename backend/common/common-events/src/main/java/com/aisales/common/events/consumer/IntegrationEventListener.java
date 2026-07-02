package com.aisales.common.events.consumer;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.MDCUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.inbox.DeadLetterService;
import com.aisales.common.events.inbox.InboxService;
import com.aisales.common.events.kafka.EventKafkaHeaderPropagator;
import com.aisales.common.events.model.BaseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.function.Consumer;

/**
 * Standard integration-event consumer pipeline: headers → idempotency → handler → inbox mark.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegrationEventListener {

    private final ObjectMapper objectMapper;
    private final InboxService inboxService;
    private final DeadLetterService deadLetterService;
    private final EventKafkaHeaderPropagator headerPropagator;

    @Transactional
    public <T extends BaseEvent> void handle(ConsumerRecord<String, String> record, String consumerName,
                                             Class<T> eventType, Consumer<T> handler) {
        String eventId = null;
        String eventTypeName = eventType.getSimpleName();
        Span consumerSpan = headerPropagator.startConsumerSpan(record, "kafka-consume-" + consumerName);
        try (AutoCloseable spanScope = headerPropagator.activateSpan(consumerSpan)) {
            headerPropagator.applyConsumerHeaders(record);
            T event = objectMapper.readValue(record.value(), eventType);
            headerPropagator.applyEventContext(event);
            MDCUtils.putContext();

            eventId = event.getEventId();
            eventTypeName = event.getEventType() != null ? event.getEventType() : eventTypeName;

            if (StringUtils.hasText(eventId) && inboxService.isProcessed(eventId, consumerName)) {
                log.debug("Skipping already processed event {} for consumer {}", eventId, consumerName);
                return;
            }

            handler.accept(event);

            if (StringUtils.hasText(eventId)) {
                inboxService.markProcessed(eventId, consumerName);
            }
        } catch (Exception ex) {
            log.error("Failed to process Kafka event for consumer {} on topic {}", consumerName, record.topic(), ex);
            deadLetterService.recordFailure(record, consumerName, eventId, eventTypeName, ex);
        } finally {
            headerPropagator.endSpan(consumerSpan);
            TenantContext.clear();
            CorrelationIdUtils.clear();
            MDCUtils.clearContext();
        }
    }
}
