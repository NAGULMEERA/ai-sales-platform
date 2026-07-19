package com.aisales.common.contracts.client;

import com.aisales.common.contracts.analytics.AnalyticsPeriod;
import com.aisales.common.contracts.analytics.DashboardSummaryDto;
import com.aisales.common.contracts.analytics.LeadFunnelDto;
import com.aisales.common.contracts.analytics.NamedCountDto;
import com.aisales.common.contracts.analytics.OpportunityPipelineDto;
import com.aisales.common.contracts.analytics.TimeSeriesPointDto;
import com.aisales.common.core.dto.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "analytics-service",
        path = "/api/v1/analytics",
        url = "${aisales.clients.analytics-service.url:}")
public interface AnalyticsServiceClient {

    @GetMapping("/dashboard")
    ApiResponse<DashboardSummaryDto> dashboard(
            @RequestParam(defaultValue = "DAILY") AnalyticsPeriod period);

    @GetMapping("/leads/funnel")
    ApiResponse<LeadFunnelDto> leadFunnel();

    @GetMapping("/leads/sources")
    ApiResponse<List<NamedCountDto>> leadSources(@RequestParam(defaultValue = "10") int limit);

    @GetMapping("/opportunities/pipeline")
    ApiResponse<OpportunityPipelineDto> opportunityPipeline();

    @GetMapping("/leads/trend")
    ApiResponse<List<TimeSeriesPointDto>> leadTrend(
            @RequestParam(defaultValue = "DAILY") AnalyticsPeriod period,
            @RequestParam(defaultValue = "30") int days);

    @GetMapping("/customers/growth")
    ApiResponse<Map<String, Object>> customerGrowth(
            @RequestParam(defaultValue = "MONTHLY") AnalyticsPeriod period);

    @GetMapping("/conversion/trends")
    ApiResponse<List<NamedCountDto>> conversionTrends(@RequestParam(defaultValue = "30") int days);

    @GetMapping("/ai/usage")
    ApiResponse<Map<String, Object>> aiUsage(
            @RequestParam(defaultValue = "DAILY") AnalyticsPeriod period);
}
