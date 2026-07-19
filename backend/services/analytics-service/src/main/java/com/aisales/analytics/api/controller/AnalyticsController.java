package com.aisales.analytics.api.controller;

import com.aisales.analytics.application.audit.AnalyticsAuditor;
import com.aisales.analytics.application.service.AnalyticsQueryService;
import com.aisales.common.contracts.analytics.AnalyticsPeriod;
import com.aisales.common.contracts.analytics.DashboardSummaryDto;
import com.aisales.common.contracts.analytics.LeadFunnelDto;
import com.aisales.common.contracts.analytics.NamedCountDto;
import com.aisales.common.contracts.analytics.OpportunityPipelineDto;
import com.aisales.common.contracts.analytics.TimeSeriesPointDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@PreAuthorizeTenant
@Tag(name = "Analytics", description = "Tenant-aware business analytics and dashboards")
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsAuditor analyticsAuditor;

    @GetMapping("/dashboard")
    @Operation(summary = "Business dashboard summary")
    public ApiResponse<DashboardSummaryDto> dashboard(
            @RequestParam(defaultValue = "DAILY") AnalyticsPeriod period) {
        analyticsAuditor.dashboardViewed(period.name());
        return ApiResponse.ok(analyticsQueryService.dashboard(period));
    }

    @GetMapping("/leads/funnel")
    @Operation(summary = "Lead funnel metrics")
    public ApiResponse<LeadFunnelDto> leadFunnel() {
        analyticsAuditor.funnelViewed();
        return ApiResponse.ok(analyticsQueryService.leadFunnel());
    }

    @GetMapping("/leads/sources")
    @Operation(summary = "Lead source analysis")
    public ApiResponse<List<NamedCountDto>> leadSources(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(analyticsQueryService.leadSources(limit));
    }

    @GetMapping("/leads/trend")
    @Operation(summary = "Daily / weekly / monthly lead trend")
    public ApiResponse<List<TimeSeriesPointDto>> leadTrend(
            @RequestParam(defaultValue = "DAILY") AnalyticsPeriod period,
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsQueryService.leadTrend(period, days));
    }

    @GetMapping("/opportunities/pipeline")
    @Operation(summary = "Opportunity pipeline")
    public ApiResponse<OpportunityPipelineDto> opportunityPipeline() {
        return ApiResponse.ok(analyticsQueryService.opportunityPipeline());
    }

    @GetMapping("/customers/growth")
    @Operation(summary = "Customer growth")
    public ApiResponse<Map<String, Object>> customerGrowth(
            @RequestParam(defaultValue = "MONTHLY") AnalyticsPeriod period) {
        return ApiResponse.ok(analyticsQueryService.customerGrowth(period));
    }

    @GetMapping("/conversion/trends")
    @Operation(summary = "Lead conversion trends")
    public ApiResponse<List<NamedCountDto>> conversionTrends(
            @RequestParam(defaultValue = "30") int days) {
        return ApiResponse.ok(analyticsQueryService.conversionTrends(days));
    }

    @GetMapping("/ai/usage")
    @Operation(summary = "AI usage and qualification accuracy")
    public ApiResponse<Map<String, Object>> aiUsage(
            @RequestParam(defaultValue = "DAILY") AnalyticsPeriod period) {
        return ApiResponse.ok(analyticsQueryService.aiUsage(period));
    }
}
