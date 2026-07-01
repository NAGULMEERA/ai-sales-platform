package com.aisales.common.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CustomMetrics {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public CustomMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void increment(String name, String... tags) {
        counter(name, tags).increment();
    }

    public Timer.Sample startTimer(String name, String... tags) {
        return Timer.start(meterRegistry);
    }

    public void recordTimer(Timer.Sample sample, String name, String... tags) {
        sample.stop(timer(name, tags));
    }

    private Counter counter(String name, String... tags) {
        String key = name + String.join(",", tags);
        return counters.computeIfAbsent(key, k -> Counter.builder(name).tags(tags).register(meterRegistry));
    }

    private Timer timer(String name, String... tags) {
        String key = name + String.join(",", tags);
        return timers.computeIfAbsent(key, k -> Timer.builder(name).tags(tags).register(meterRegistry));
    }
}
