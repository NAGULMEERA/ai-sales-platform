package com.aisales.analytics.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aisales.analytics.domain.AnalyticsMetricNames;
import com.aisales.analytics.infrastructure.persistence.AnalyticsEventRepository;
import com.aisales.common.contracts.analytics.AnalyticsPeriod;
import com.aisales.common.contracts.analytics.DashboardSummaryDto;
import com.aisales.common.contracts.analytics.LeadFunnelDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceTest {

    @Mock
    private AnalyticsEventRepository eventRepository;
    @Mock
    private ObjectProvider<PlatformMetrics> platformMetrics;

    private AnalyticsQueryService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        lenient().when(platformMetrics.getIfAvailable()).thenReturn(null);
        service = new AnalyticsQueryService(eventRepository, platformMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldBuildLeadFunnelFromFacts() {
        when(eventRepository.countMetric(eq(tenantId), eq(AnalyticsMetricNames.LEAD_CREATED), any(), any()))
                .thenReturn(100L);
        when(eventRepository.countMetric(eq(tenantId), eq(AnalyticsMetricNames.LEAD_QUALIFIED), any(), any()))
                .thenReturn(40L);
        when(eventRepository.countMetric(eq(tenantId), eq(AnalyticsMetricNames.LEAD_CONVERTED), any(), any()))
                .thenReturn(10L);
        when(eventRepository.countMetric(eq(tenantId), eq(AnalyticsMetricNames.LEAD_LOST), any(), any()))
                .thenReturn(5L);
        when(eventRepository.countByStatus(eq(tenantId), eq(AnalyticsMetricNames.LEAD_STATUS_CHANGED), any(), any()))
                .thenReturn(List.of(new Object[] {"CONTACTED", 20L}, new Object[] {"VALIDATED", 50L}));

        LeadFunnelDto funnel = service.leadFunnel();

        assertThat(funnel.getCreated()).isEqualTo(100L);
        assertThat(funnel.getValidated()).isEqualTo(50L);
        assertThat(funnel.getQualified()).isEqualTo(40L);
        assertThat(funnel.getConverted()).isEqualTo(10L);
        assertThat(funnel.getConversionRate()).isEqualTo(0.1);
        assertThat(funnel.getStages()).isNotEmpty();
        assertThat(funnel.getStages().get(1).getStage()).isEqualTo("VALIDATED");
        assertThat(funnel.getStages().get(1).getCount()).isEqualTo(50L);
    }

    @Test
    void shouldBuildDashboardSummary() {
        List<Object[]> grouped = List.<Object[]>of(
                new Object[] {AnalyticsMetricNames.LEAD_CREATED, 2L});
        when(eventRepository.countMetricsGrouped(any(UUID.class), any(), any(), any())).thenReturn(grouped);
        when(eventRepository.countMetric(any(UUID.class), anyString(), any(), any())).thenReturn(2L);
        when(eventRepository.avgMetric(any(UUID.class), anyString(), any(), any())).thenReturn(0.85);
        when(eventRepository.dailySeries(any(UUID.class), anyString(), any(), any())).thenReturn(List.of());
        when(eventRepository.topDimensions(any(UUID.class), anyString(), anyString(), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(eventRepository.countByStatus(any(UUID.class), anyString(), any(), any())).thenReturn(List.of());

        DashboardSummaryDto dashboard = service.dashboard(AnalyticsPeriod.DAILY);

        assertThat(dashboard.getLeadsToday()).isEqualTo(2L);
        assertThat(dashboard.getAiQualificationAccuracy()).isEqualTo(0.85);
        assertThat(dashboard.getTenantUsage()).containsKey("period");
    }
}
