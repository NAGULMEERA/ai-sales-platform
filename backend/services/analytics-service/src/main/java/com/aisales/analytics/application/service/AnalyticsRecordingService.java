package com.aisales.analytics.application.service;

import com.aisales.analytics.domain.entity.AnalyticsDailyRollup;
import com.aisales.analytics.domain.entity.AnalyticsEvent;
import com.aisales.analytics.infrastructure.persistence.AnalyticsDailyRollupRepository;
import com.aisales.analytics.infrastructure.persistence.AnalyticsEventRepository;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnalyticsRecordingService {

    private final AnalyticsEventRepository eventRepository;
    private final AnalyticsDailyRollupRepository rollupRepository;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public void record(
            String tenantId,
            String metricName,
            double metricValue,
            Instant occurredAt,
            String correlationId,
            Map<String, Object> dimensions) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(metricName)) {
            return;
        }
        UUID tenantUuid = UUID.fromString(tenantId);
        Instant at = occurredAt == null ? Instant.now() : occurredAt;
        Instant now = Instant.now();
        Map<String, Object> dims = dimensions == null ? new HashMap<>() : new HashMap<>(dimensions);

        AnalyticsEvent event = AnalyticsEvent.builder()
                .tenantId(tenantUuid)
                .metricName(metricName)
                .metricValue(metricValue)
                .dimensions(dims)
                .occurredAt(at)
                .correlationId(correlationId)
                .createdAt(now)
                .updatedAt(now)
                .createdBy("system")
                .updatedBy("system")
                .build();
        eventRepository.save(event);

        LocalDate day = LocalDate.ofInstant(at, ZoneOffset.UTC);
        AnalyticsDailyRollup rollup = rollupRepository
                .findByTenantIdAndMetricDateAndMetricName(tenantUuid, day, metricName)
                .orElseGet(() -> AnalyticsDailyRollup.builder()
                        .tenantId(tenantUuid)
                        .metricDate(day)
                        .metricName(metricName)
                        .metricSum(0d)
                        .metricCount(0L)
                        .dimensions(Map.of())
                        .createdAt(now)
                        .build());
        rollup.setMetricSum(rollup.getMetricSum() + metricValue);
        rollup.setMetricCount(rollup.getMetricCount() + 1);
        rollup.setUpdatedAt(now);
        rollupRepository.save(rollup);

        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementBusinessMetric(
                    MetricNames.ANALYTICS_EVENT_RECORDED, tenantId, "metric", metricName);
        }
    }

    @Transactional
    public void recordCount(
            String tenantId,
            String metricName,
            Instant occurredAt,
            String correlationId,
            Map<String, Object> dimensions) {
        record(tenantId, metricName, 1d, occurredAt, correlationId, dimensions);
    }
}
