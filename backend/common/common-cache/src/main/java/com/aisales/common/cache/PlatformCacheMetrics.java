package com.aisales.common.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.util.function.Supplier;

public class PlatformCacheMetrics {

    private final MeterRegistry meterRegistry;

    public PlatformCacheMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordHit(String namespace) {
        counter("cache.hit", namespace).increment();
    }

    public void recordMiss(String namespace) {
        counter("cache.miss", namespace).increment();
    }

    public void recordPut(String namespace) {
        counter("cache.put", namespace).increment();
    }

    public void recordEviction(String namespace, long count) {
        counter("cache.eviction", namespace).increment(count);
    }

    public void recordLoadFailure(String namespace) {
        counter("cache.load.failure", namespace).increment();
    }

    public <T> T recordLatency(String operation, String namespace, Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return supplier.get();
        } finally {
            sample.stop(Timer.builder("cache.operation.latency")
                    .description("Cache operation latency")
                    .tags(tags(namespace).and("operation", operation))
                    .register(meterRegistry));
        }
    }

    private Counter counter(String name, String namespace) {
        return Counter.builder(name)
                .description("Platform cache " + name.substring("cache.".length()))
                .tags(tags(namespace))
                .register(meterRegistry);
    }

    private Tags tags(String namespace) {
        return Tags.of("cache_namespace", namespace);
    }
}
