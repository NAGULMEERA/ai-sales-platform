package com.aisales.common.events.outbox;

import com.aisales.common.observability.metrics.MetricNames;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
@ConditionalOnBean(MeterRegistry.class)
public class OutboxMetricsBinder {

    private final OutboxRepository outboxRepository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    void bind() {
        Gauge.builder(MetricNames.OUTBOX_PENDING, outboxRepository,
                        repo -> repo.countByStatus(OutboxEvent.OutboxStatus.PENDING))
                .description("Pending outbox events awaiting dispatch")
                .register(meterRegistry);
    }
}
