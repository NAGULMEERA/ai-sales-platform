package com.aisales.common.events.publisher;

import com.aisales.common.events.model.BaseEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * Local-dev publisher: logs events instead of sending to Kafka.
 */
@Slf4j
public class LoggingEventPublisher implements EventPublisher {

    @Override
    public void publish(BaseEvent event) {
        publish("aisales-events", event);
    }

    @Override
    public void publish(String topic, BaseEvent event) {
        log.info("Event published (local/logging): topic={}, type={}, tenantId={}, aggregateId={}",
                topic, event.getEventType(), event.getTenantId(), event.getAggregateId());
    }
}
