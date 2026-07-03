package com.aisales.common.events.publisher;

import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import lombok.extern.slf4j.Slf4j;

/**
 * Local-dev publisher: logs events instead of sending to Kafka.
 */
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    private final PlatformMetrics platformMetrics;

    public LoggingEventPublisher() {
        this(null);
    }

    public LoggingEventPublisher(PlatformMetrics platformMetrics) {
        this.platformMetrics = platformMetrics;
    }

    @Override
    public void publish(BaseEvent event) {
        publish("aisales-events", event);
    }

    @Override
    public void publish(String topic, BaseEvent event) {
        log.info("Event published (local/logging): topic={}, type={}, tenantId={}, aggregateId={}",
                topic, event.getEventType(), event.getTenantId(), event.getAggregateId());
        recordPublished(event, topic);
    }

    private void recordPublished(BaseEvent event, String topic) {
        if (platformMetrics != null) {
            platformMetrics.incrementBusinessMetric(MetricNames.EVENT_PUBLISHED, event.getTenantId(),
                    "event_type", event.getEventType(), "topic", topic, "publisher", "logging");
        }
    }
}
