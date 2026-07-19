package com.aisales.common.contracts.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDto {

    private long leadsToday;
    private long leadsThisWeek;
    private long leadsThisMonth;
    private long customersCreated;
    private long opportunitiesOpen;
    private long opportunitiesWon;
    private long conversationsStarted;
    private long workflowsExecuted;
    private long workflowsFailed;
    private long aiRequests;
    private long searchRequests;
    private long catalogMatches;
    private Double leadConversionRate;
    private Double opportunityConversionRate;
    private Double workflowSuccessRate;
    private Double aiQualificationAccuracy;
    private Double catalogRecommendationAccuracy;
    private Double avgConversationResponseSeconds;

    @Builder.Default
    private List<TimeSeriesPointDto> leadTrend = new ArrayList<>();

    @Builder.Default
    private List<FunnelStageDto> salesFunnel = new ArrayList<>();

    @Builder.Default
    private List<NamedCountDto> topProducts = new ArrayList<>();

    @Builder.Default
    private List<NamedCountDto> leadSources = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> tenantUsage = new HashMap<>();
}
