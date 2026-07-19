package com.aisales.analytics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.analytics.domain.AnalyticsMetricNames;
import com.aisales.analytics.domain.entity.AnalyticsEvent;
import com.aisales.analytics.infrastructure.persistence.AnalyticsDailyRollupRepository;
import com.aisales.analytics.infrastructure.persistence.AnalyticsEventRepository;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AnalyticsRecordingServiceTest {

    @Mock
    private AnalyticsEventRepository eventRepository;
    @Mock
    private AnalyticsDailyRollupRepository rollupRepository;
    @Mock
    private ObjectProvider<PlatformMetrics> platformMetrics;
    @Mock
    private PlatformMetrics metrics;

    private AnalyticsRecordingService service;

    @BeforeEach
    void setUp() {
        when(platformMetrics.getIfAvailable()).thenReturn(metrics);
        when(eventRepository.save(any(AnalyticsEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rollupRepository.upsertDailyRollup(any(), any(), any(), any(), anyDouble())).thenReturn(1);
        service = new AnalyticsRecordingService(eventRepository, rollupRepository, platformMetrics);
    }

    @Test
    void shouldRecordEventAndUpsertDailyRollup() {
        UUID tenantId = UUID.randomUUID();
        Instant at = Instant.parse("2026-07-19T10:00:00Z");

        service.recordCount(
                tenantId.toString(),
                AnalyticsMetricNames.LEAD_CREATED,
                at,
                "corr-1",
                Map.of("source", "WEB"));

        ArgumentCaptor<AnalyticsEvent> eventCaptor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        AnalyticsEvent saved = eventCaptor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(tenantId);
        assertThat(saved.getMetricName()).isEqualTo(AnalyticsMetricNames.LEAD_CREATED);
        assertThat(saved.getMetricValue()).isEqualTo(1d);
        assertThat(saved.getDimensions()).containsEntry("source", "WEB");
        assertThat(saved.getCorrelationId()).isEqualTo("corr-1");

        verify(rollupRepository).upsertDailyRollup(
                any(UUID.class),
                eq(tenantId),
                eq(LocalDate.ofInstant(at, ZoneOffset.UTC)),
                eq(AnalyticsMetricNames.LEAD_CREATED),
                eq(1d));
    }
}
