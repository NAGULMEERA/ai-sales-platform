package com.aisales.common.observability.resilience;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;

/**
 * Logs bulkhead rejections so capacity saturation is visible in structured logs.
 */
@Slf4j
@Configuration
@ConditionalOnBean(BulkheadRegistry.class)
@RequiredArgsConstructor
public class BulkheadObservabilityConfig {

    private final BulkheadRegistry bulkheadRegistry;

    @PostConstruct
    void registerEventConsumers() {
        bulkheadRegistry.getAllBulkheads().forEach(this::attach);
        bulkheadRegistry.getEventPublisher().onEntryAdded(event -> attach(event.getAddedEntry()));
    }

    private void attach(Bulkhead bulkhead) {
        bulkhead.getEventPublisher().onCallRejected(this::onRejected);
    }

    private void onRejected(BulkheadOnCallRejectedEvent event) {
        log.warn("Bulkhead rejected call name={} ", event.getBulkheadName());
    }
}
