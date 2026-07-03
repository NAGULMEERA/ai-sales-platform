package com.aisales.common.events.publisher;

import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "false", matchIfMissing = true)
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final PlatformMetrics platformMetrics;

    @Value("${aisales.events.default-topic:aisales-events}")
    private String defaultTopic;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this(kafkaTemplate, objectMapper, null);
    }

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper,
                               PlatformMetrics platformMetrics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.platformMetrics = platformMetrics;
    }

    @Override
    public void publish(BaseEvent event) {
        publish(defaultTopic, event);
    }

    @Override
    public void publish(String topic, BaseEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = event.getTenantId() != null ? event.getTenantId() : event.getAggregateId();
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {} to topic {}", event.getEventType(), topic, ex);
                        } else {
                            log.debug("Published event {} to topic {}", event.getEventType(), topic);
                            recordPublished(event, topic);
                        }
                    });
        } catch (JsonProcessingException e) {
            throw new com.aisales.common.exception.exception.EventPublishException(
                    "Failed to serialize event: " + event.getEventType(), e);
        }
    }

    private void recordPublished(BaseEvent event, String topic) {
        if (platformMetrics != null) {
            platformMetrics.incrementBusinessMetric(MetricNames.EVENT_PUBLISHED, event.getTenantId(),
                    "event_type", event.getEventType(), "topic", topic, "publisher", "kafka");
        }
    }
}
