package com.aisales.common.observability.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformMetricsTest {

    @Test
    void shouldIncrementBusinessCounterWithTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        PlatformMetrics metrics = new PlatformMetrics(registry);

        metrics.increment(MetricNames.TENANT_CREATED);
        metrics.incrementForTenant(MetricNames.LEAD_CREATED, "tenant-1");

        assertThat(registry.get(MetricNames.TENANT_CREATED).counter().count()).isEqualTo(1.0);
        assertThat(registry.get(MetricNames.LEAD_CREATED).tag("tenant_id", "tenant-1").counter().count())
                .isEqualTo(1.0);
    }
}
