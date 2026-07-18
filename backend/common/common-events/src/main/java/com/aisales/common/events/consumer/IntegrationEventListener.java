package com.aisales.common.events.consumer;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.MDCUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.inbox.DeadLetterService;
import com.aisales.common.events.inbox.InboxService;
import com.aisales.common.events.kafka.EventKafkaHeaderPropagator;
import com.aisales.common.events.kafka.EventKafkaHeaders;
import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Span;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Standard integration-event consumer pipeline: headers → idempotency → handler → inbox mark.
 */
@Slf4j
@Component
public class IntegrationEventListener {

    private final ObjectMapper objectMapper;
    private final InboxService inboxService;
    private final DeadLetterService deadLetterService;
    private final EventKafkaHeaderPropagator headerPropagator;
    private final TransactionTemplate transactionTemplate;
    private final PlatformMetrics platformMetrics;

    @Value("${aisales.events.consumer.max-attempts:3}")
    private int maxAttempts;

    public IntegrationEventListener(ObjectMapper objectMapper,
                                    InboxService inboxService,
                                    DeadLetterService deadLetterService,
                                    EventKafkaHeaderPropagator headerPropagator,
                                    PlatformTransactionManager transactionManager) {
        this.objectMapper = objectMapper;
        this.inboxService = inboxService;
        this.deadLetterService = deadLetterService;
        this.headerPropagator = headerPropagator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.platformMetrics = null;
    }

    @Autowired
    public IntegrationEventListener(ObjectMapper objectMapper,
                                    InboxService inboxService,
                                    DeadLetterService deadLetterService,
                                    EventKafkaHeaderPropagator headerPropagator,
                                    PlatformTransactionManager transactionManager,
                                    ObjectProvider<PlatformMetrics> platformMetrics) {
        this.objectMapper = objectMapper;
        this.inboxService = inboxService;
        this.deadLetterService = deadLetterService;
        this.headerPropagator = headerPropagator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.platformMetrics = platformMetrics.getIfAvailable();
    }

    /**
     * Processes a record only when its {@code eventType} matches {@code expectedEventType}.
     * Shared topics can carry many event types; unmatched records are ignored (not DLQ'd).
     */
    public <T extends BaseEvent> void handleIfType(ConsumerRecord<String, String> record, String consumerName,
                                                   String expectedEventType, Class<T> eventType,
                                                   Consumer<T> handler) {
        String actualType = resolveEventType(record);
        if (!StringUtils.hasText(actualType) || !expectedEventType.equals(actualType)) {
            return;
        }
        handle(record, consumerName, eventType, handler);
    }

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
            validateEventEnvelope(event);

            if (inboxService.isProcessed(eventId, consumerName)) {
                log.debug("Skipping already processed event {} for consumer {}", eventId, consumerName);
                return;
            }

            processWithBoundedRetries(record, consumerName, event, handler);
            recordConsumed(event, consumerName, record.topic());
        } catch (Exception ex) {
            log.error("Failed to process Kafka event for consumer {} on topic {}", consumerName, record.topic(), ex);
            deadLetterService.recordFailure(record, consumerName, eventId, eventTypeName, 0, ex);
            recordDlq(eventTypeName, null, consumerName, record.topic());
        } finally {
            headerPropagator.endSpan(consumerSpan);
            TenantContext.clear();
            CorrelationIdUtils.clear();
            MDCUtils.clearContext();
        }
    }

    private String resolveEventType(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader(EventKafkaHeaders.EVENT_TYPE);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        try {
            JsonNode node = objectMapper.readTree(record.value());
            return node.path("eventType").asText(null);
        } catch (Exception ex) {
            log.debug("Unable to resolve eventType from Kafka record on topic {}", record.topic(), ex);
            return null;
        }
    }

    private <T extends BaseEvent> void processWithBoundedRetries(ConsumerRecord<String, String> record,
                                                                 String consumerName,
                                                                 T event,
                                                                 Consumer<T> handler) {
        int attempts = Math.max(1, maxAttempts);
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    handler.accept(event);
                    inboxService.markProcessed(event.getEventId(), consumerName);
                });
                if (attempt > 1) {
                    log.info("Kafka event {} processed after {} attempts", event.getEventId(), attempt);
                }
                return;
            } catch (Exception ex) {
                lastFailure = ex;
                if (attempt < attempts) {
                    log.warn("Kafka event {} processing failed for consumer {} (attempt {}/{}): {}",
                            event.getEventId(), consumerName, attempt, attempts, ex.getMessage());
                }
            }
        }
        log.error("Kafka event {} exhausted {} processing attempts for consumer {}",
                event.getEventId(), attempts, consumerName, lastFailure);
        deadLetterService.recordFailure(record, consumerName, event.getEventId(), event.getEventType(), attempts,
                lastFailure);
        recordDlq(event.getEventType(), event.getTenantId(), consumerName, record.topic());
    }

    private static void validateEventEnvelope(BaseEvent event) {
        if (!StringUtils.hasText(event.getEventId())) {
            throw new IllegalArgumentException("Integration event is missing eventId");
        }
        if (event.getEventVersion() < 1) {
            throw new IllegalArgumentException("Integration event is missing eventVersion");
        }
    }

    private void recordConsumed(BaseEvent event, String consumerName, String topic) {
        if (platformMetrics != null) {
            platformMetrics.incrementBusinessMetric(MetricNames.EVENT_CONSUMED, event.getTenantId(),
                    "event_type", event.getEventType(), "consumer", consumerName, "topic", topic);
        }
    }

    private void recordDlq(String eventType, String tenantId, String consumerName, String topic) {
        if (platformMetrics != null) {
            platformMetrics.incrementBusinessMetric(MetricNames.EVENT_DLQ, tenantId,
                    "event_type", eventType, "consumer", consumerName, "topic", topic);
        }
    }
}
