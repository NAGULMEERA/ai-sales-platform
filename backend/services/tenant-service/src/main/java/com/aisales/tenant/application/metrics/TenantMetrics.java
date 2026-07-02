package com.aisales.tenant.application.metrics;

import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantMetrics {

    private final PlatformMetrics platformMetrics;

    public void recordTenantCreated() {
        platformMetrics.increment(MetricNames.TENANT_CREATED);
    }

    public void recordTenantUpdated() {
        platformMetrics.increment(MetricNames.TENANT_UPDATED);
    }

    public void recordTenantDeleted() {
        platformMetrics.increment(MetricNames.TENANT_DELETED);
    }
}
