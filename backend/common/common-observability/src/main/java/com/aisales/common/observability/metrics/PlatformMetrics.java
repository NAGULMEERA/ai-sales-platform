package com.aisales.common.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Standard platform metrics helper for business KPI counters and timers.
 * Prefer {@link MetricNames} for metric name consistency across services.
 */
@Component
public class PlatformMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public PlatformMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(String metricName, String... tags) {
        counter(metricName, tags).increment();
    }

    public void incrementForTenant(String metricName, String tenantId) {
        if (StringUtils.hasText(tenantId)) {
            increment(metricName, "tenant_id", tenantId);
        } else {
            increment(metricName);
        }
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordTimer(Timer.Sample sample, String metricName, String... tags) {
        sample.stop(timer(metricName, tags));
    }

    private Counter counter(String metricName, String... tags) {
        String key = metricName + String.join(",", tags);
        return counters.computeIfAbsent(key, k -> Counter.builder(metricName).tags(tags).register(meterRegistry));
    }

    private Timer timer(String metricName, String... tags) {
        String key = metricName + String.join(",", tags);
        return timers.computeIfAbsent(key, k -> Timer.builder(metricName).tags(tags).register(meterRegistry));
    }
}
