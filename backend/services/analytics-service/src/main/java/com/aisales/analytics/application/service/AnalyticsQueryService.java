package com.aisales.analytics.application.service;

import com.aisales.analytics.domain.AnalyticsMetricNames;
import com.aisales.analytics.infrastructure.persistence.AnalyticsEventRepository;
import com.aisales.common.contracts.analytics.AnalyticsPeriod;
import com.aisales.common.contracts.analytics.DashboardSummaryDto;
import com.aisales.common.contracts.analytics.FunnelStageDto;
import com.aisales.common.contracts.analytics.LeadFunnelDto;
import com.aisales.common.contracts.analytics.NamedCountDto;
import com.aisales.common.contracts.analytics.OpportunityPipelineDto;
import com.aisales.common.contracts.analytics.TimeSeriesPointDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryService {

    private final AnalyticsEventRepository eventRepository;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional(readOnly = true)
    public DashboardSummaryDto dashboard(AnalyticsPeriod period) {
        UUID tenantId = requireTenantId();
        Instant now = Instant.now();
        Instant monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant weekStart = now.minus(7, ChronoUnit.DAYS);
        Instant dayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant periodStart = resolvePeriodStart(period, now);

        long leadsToday = count(tenantId, AnalyticsMetricNames.LEAD_CREATED, dayStart, now);
        long leadsWeek = count(tenantId, AnalyticsMetricNames.LEAD_CREATED, weekStart, now);
        long leadsMonth = count(tenantId, AnalyticsMetricNames.LEAD_CREATED, monthStart, now);
        long customers = count(tenantId, AnalyticsMetricNames.CUSTOMER_CREATED, periodStart, now);
        long oppCreated = count(tenantId, AnalyticsMetricNames.OPPORTUNITY_CREATED, periodStart, now);
        long oppWon = count(tenantId, AnalyticsMetricNames.OPPORTUNITY_WON, periodStart, now);
        long oppLost = count(tenantId, AnalyticsMetricNames.OPPORTUNITY_LOST, periodStart, now);
        long conversations = count(tenantId, AnalyticsMetricNames.CONVERSATION_STARTED, periodStart, now);
        long workflowsExecuted = count(tenantId, AnalyticsMetricNames.WORKFLOW_EXECUTED, periodStart, now);
        long workflowsCompleted = count(tenantId, AnalyticsMetricNames.WORKFLOW_COMPLETED, periodStart, now);
        long workflowsFailed = count(tenantId, AnalyticsMetricNames.WORKFLOW_FAILED, periodStart, now);
        long aiRequests = count(tenantId, AnalyticsMetricNames.AI_REQUEST, periodStart, now)
                + count(tenantId, AnalyticsMetricNames.AI_QUALIFICATION, periodStart, now);
        long searchRequests = count(tenantId, AnalyticsMetricNames.SEARCH_REQUEST, periodStart, now);
        long catalogMatches = count(tenantId, AnalyticsMetricNames.CATALOG_MATCH, periodStart, now);
        long leadsConverted = count(tenantId, AnalyticsMetricNames.LEAD_CONVERTED, periodStart, now);
        long leadsCreatedPeriod = count(tenantId, AnalyticsMetricNames.LEAD_CREATED, periodStart, now);

        Double aiAccuracy = avgOrNull(tenantId, AnalyticsMetricNames.AI_QUALIFICATION_ACCURACY, periodStart, now);
        Double catalogAccuracy =
                avgOrNull(tenantId, AnalyticsMetricNames.CATALOG_RECOMMENDATION_ACCURACY, periodStart, now);
        Double avgResponse =
                avgOrNull(tenantId, AnalyticsMetricNames.CONVERSATION_RESPONSE_SECONDS, periodStart, now);

        List<TimeSeriesPointDto> leadTrend = leadTrend(period, daysForPeriod(period));
        LeadFunnelDto funnel = leadFunnel();
        OpportunityPipelineDto pipeline = opportunityPipeline();

        Map<String, Object> tenantUsage = new HashMap<>();
        tenantUsage.put("period", period.name());
        tenantUsage.put("leads", leadsCreatedPeriod);
        tenantUsage.put("customers", customers);
        tenantUsage.put("opportunities", oppCreated);
        tenantUsage.put("conversations", conversations);
        tenantUsage.put("aiRequests", aiRequests);
        tenantUsage.put("searchRequests", searchRequests);

        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementBusinessMetric(
                    MetricNames.ANALYTICS_DASHBOARD, tenantId.toString(), "period", period.name());
        }

        return DashboardSummaryDto.builder()
                .leadsToday(leadsToday)
                .leadsThisWeek(leadsWeek)
                .leadsThisMonth(leadsMonth)
                .customersCreated(customers)
                .opportunitiesOpen(Math.max(0, oppCreated - oppWon - oppLost))
                .opportunitiesWon(oppWon)
                .conversationsStarted(conversations)
                .workflowsExecuted(workflowsExecuted)
                .workflowsFailed(workflowsFailed)
                .aiRequests(aiRequests)
                .searchRequests(searchRequests)
                .catalogMatches(catalogMatches)
                .leadConversionRate(rate(leadsConverted, leadsCreatedPeriod))
                .opportunityConversionRate(rate(oppWon, oppCreated))
                .workflowSuccessRate(rate(workflowsCompleted, workflowsExecuted))
                .aiQualificationAccuracy(aiAccuracy)
                .catalogRecommendationAccuracy(catalogAccuracy)
                .avgConversationResponseSeconds(avgResponse)
                .leadTrend(leadTrend)
                .salesFunnel(funnel.getStages())
                .topProducts(topProducts(periodStart, now, 10))
                .leadSources(leadSources(periodStart, now, 10))
                .tenantUsage(tenantUsage)
                .build();
    }

    @Transactional(readOnly = true)
    public LeadFunnelDto leadFunnel() {
        UUID tenantId = requireTenantId();
        Instant from = Instant.EPOCH;
        Instant to = Instant.now();
        long created = count(tenantId, AnalyticsMetricNames.LEAD_CREATED, from, to);
        long qualified = count(tenantId, AnalyticsMetricNames.LEAD_QUALIFIED, from, to);
        long converted = count(tenantId, AnalyticsMetricNames.LEAD_CONVERTED, from, to);
        long lost = count(tenantId, AnalyticsMetricNames.LEAD_LOST, from, to);

        Map<String, Long> byStatus = toNamedMap(
                eventRepository.countByStatus(tenantId, AnalyticsMetricNames.LEAD_STATUS_CHANGED, from, to));
        long validated = byStatus.getOrDefault("VALIDATED", 0L)
                + byStatus.getOrDefault("NEW", 0L);
        long contacted = byStatus.getOrDefault("CONTACTED", 0L)
                + byStatus.getOrDefault("IN_PROGRESS", 0L);

        List<FunnelStageDto> stages = buildFunnelStages(List.of(
                Map.entry("CREATED", created),
                Map.entry("VALIDATED", Math.max(validated, created > 0 ? created : 0)),
                Map.entry("QUALIFIED", qualified),
                Map.entry("CONTACTED", contacted),
                Map.entry("CONVERTED", converted),
                Map.entry("LOST", lost)));

        return LeadFunnelDto.builder()
                .created(created)
                .validated(Math.max(validated, 0))
                .qualified(qualified)
                .contacted(contacted)
                .converted(converted)
                .lost(lost)
                .conversionRate(rate(converted, created))
                .stages(stages)
                .build();
    }

    @Transactional(readOnly = true)
    public OpportunityPipelineDto opportunityPipeline() {
        UUID tenantId = requireTenantId();
        Instant from = Instant.EPOCH;
        Instant to = Instant.now();
        long created = count(tenantId, AnalyticsMetricNames.OPPORTUNITY_CREATED, from, to);
        long won = count(tenantId, AnalyticsMetricNames.OPPORTUNITY_WON, from, to);
        long lost = count(tenantId, AnalyticsMetricNames.OPPORTUNITY_LOST, from, to);
        long open = Math.max(0, created - won - lost);

        List<NamedCountDto> byStatus = new ArrayList<>();
        byStatus.add(NamedCountDto.builder().name("OPEN").count(open).build());
        byStatus.add(NamedCountDto.builder().name("WON").count(won).build());
        byStatus.add(NamedCountDto.builder().name("LOST").count(lost).build());
        for (Object[] row : eventRepository.countByStatus(
                tenantId, AnalyticsMetricNames.OPPORTUNITY_STATUS_CHANGED, from, to)) {
            byStatus.add(NamedCountDto.builder()
                    .name(String.valueOf(row[0]))
                    .count(((Number) row[1]).longValue())
                    .build());
        }

        return OpportunityPipelineDto.builder()
                .openCount(open)
                .wonCount(won)
                .lostCount(lost)
                .openAmount(BigDecimal.ZERO)
                .wonAmount(BigDecimal.ZERO)
                .conversionRate(rate(won, created))
                .byStatus(byStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public List<TimeSeriesPointDto> leadTrend(AnalyticsPeriod period, int days) {
        UUID tenantId = requireTenantId();
        int safeDays = Math.min(Math.max(days, 1), 366);
        Instant to = Instant.now();
        Instant from = to.minus(safeDays, ChronoUnit.DAYS);
        List<TimeSeriesPointDto> points = new ArrayList<>();
        for (Object[] row : eventRepository.dailySeries(
                tenantId, AnalyticsMetricNames.LEAD_CREATED, from, to)) {
            LocalDate date = toLocalDate(row[0]);
            points.add(TimeSeriesPointDto.builder()
                    .date(date)
                    .metric(AnalyticsMetricNames.LEAD_CREATED)
                    .value(((Number) row[1]).longValue())
                    .build());
        }
        return points;
    }

    @Transactional(readOnly = true)
    public List<NamedCountDto> leadSources(int limit) {
        UUID tenantId = requireTenantId();
        Instant to = Instant.now();
        Instant from = to.minus(90, ChronoUnit.DAYS);
        return leadSources(from, to, limit);
    }

    @Transactional(readOnly = true)
    public List<NamedCountDto> conversionTrends(int days) {
        UUID tenantId = requireTenantId();
        int safeDays = Math.min(Math.max(days, 1), 366);
        Instant to = Instant.now();
        Instant from = to.minus(safeDays, ChronoUnit.DAYS);
        List<NamedCountDto> trends = new ArrayList<>();
        for (Object[] row : eventRepository.dailySeries(
                tenantId, AnalyticsMetricNames.LEAD_CONVERTED, from, to)) {
            trends.add(NamedCountDto.builder()
                    .name(String.valueOf(toLocalDate(row[0])))
                    .count(((Number) row[1]).longValue())
                    .build());
        }
        return trends;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> aiUsage(AnalyticsPeriod period) {
        UUID tenantId = requireTenantId();
        Instant now = Instant.now();
        Instant from = resolvePeriodStart(period, now);
        Map<String, Object> usage = new HashMap<>();
        usage.put("aiRequests", count(tenantId, AnalyticsMetricNames.AI_REQUEST, from, now)
                + count(tenantId, AnalyticsMetricNames.AI_QUALIFICATION, from, now));
        usage.put("qualifications", count(tenantId, AnalyticsMetricNames.AI_QUALIFICATION, from, now));
        usage.put("ragRequests", count(tenantId, AnalyticsMetricNames.RAG_REQUEST, from, now));
        usage.put(
                "qualificationAccuracy",
                avgOrNull(tenantId, AnalyticsMetricNames.AI_QUALIFICATION_ACCURACY, from, now));
        return usage;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> customerGrowth(AnalyticsPeriod period) {
        UUID tenantId = requireTenantId();
        Instant now = Instant.now();
        Instant from = resolvePeriodStart(period, now);
        Map<String, Object> growth = new HashMap<>();
        growth.put("customersCreated", count(tenantId, AnalyticsMetricNames.CUSTOMER_CREATED, from, now));
        growth.put("leadsConverted", count(tenantId, AnalyticsMetricNames.LEAD_CONVERTED, from, now));
        growth.put("trend", leadTrend(period, daysForPeriod(period)));
        return growth;
    }

    private List<NamedCountDto> leadSources(Instant from, Instant to, int limit) {
        UUID tenantId = requireTenantId();
        List<NamedCountDto> sources = new ArrayList<>();
        for (Object[] row : eventRepository.topDimensions(
                tenantId, AnalyticsMetricNames.LEAD_CREATED, "source", from, to, Math.min(Math.max(limit, 1), 50))) {
            sources.add(NamedCountDto.builder()
                    .name(String.valueOf(row[0]))
                    .count(((Number) row[1]).longValue())
                    .build());
        }
        return sources;
    }

    private List<NamedCountDto> topProducts(Instant from, Instant to, int limit) {
        UUID tenantId = requireTenantId();
        List<NamedCountDto> products = new ArrayList<>();
        for (Object[] row : eventRepository.topDimensions(
                tenantId,
                AnalyticsMetricNames.CATALOG_RECOMMENDATION,
                "topProductId",
                from,
                to,
                Math.min(Math.max(limit, 1), 50))) {
            products.add(NamedCountDto.builder()
                    .name(String.valueOf(row[0]))
                    .count(((Number) row[1]).longValue())
                    .build());
        }
        return products;
    }

    private long count(UUID tenantId, String metric, Instant from, Instant to) {
        return eventRepository.countMetric(tenantId, metric, from, to);
    }

    private Double avgOrNull(UUID tenantId, String metric, Instant from, Instant to) {
        Double avg = eventRepository.avgMetric(tenantId, metric, from, to);
        if (avg == null || avg == 0d) {
            long n = count(tenantId, metric, from, to);
            return n == 0 ? null : avg;
        }
        return avg;
    }

    private static Double rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return null;
        }
        return (double) numerator / (double) denominator;
    }

    private static Instant resolvePeriodStart(AnalyticsPeriod period, Instant now) {
        AnalyticsPeriod p = period == null ? AnalyticsPeriod.DAILY : period;
        return switch (p) {
            case WEEKLY -> now.minus(7, ChronoUnit.DAYS);
            case MONTHLY -> LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            case DAILY -> LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        };
    }

    private static int daysForPeriod(AnalyticsPeriod period) {
        AnalyticsPeriod p = period == null ? AnalyticsPeriod.DAILY : period;
        return switch (p) {
            case DAILY -> 30;
            case WEEKLY -> 84;
            case MONTHLY -> 365;
        };
    }

    private static List<FunnelStageDto> buildFunnelStages(List<Map.Entry<String, Long>> stages) {
        List<FunnelStageDto> result = new ArrayList<>();
        Long previous = null;
        for (Map.Entry<String, Long> stage : stages) {
            Double conv = previous == null || previous == 0
                    ? null
                    : (double) stage.getValue() / (double) previous;
            result.add(FunnelStageDto.builder()
                    .stage(stage.getKey())
                    .count(stage.getValue())
                    .conversionFromPrevious(conv)
                    .build());
            previous = stage.getValue();
        }
        return result;
    }

    private static Map<String, Long> toNamedMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        if (rows == null) {
            return map;
        }
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] != null) {
                map.put(String.valueOf(row[0]).toUpperCase(), ((Number) row[1]).longValue());
            }
        }
        return map;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
