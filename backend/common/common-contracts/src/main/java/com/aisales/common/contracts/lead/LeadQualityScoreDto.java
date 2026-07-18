package com.aisales.common.contracts.lead;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadQualityScoreDto {
    private UUID id;
    private UUID leadId;
    private Integer overallScore;
    private String budgetFit;
    private String timeline;
    private String decisionMaker;
    private String competitorAwareness;
    private List<String> objections;
    private String suggestedProduct;
    private String nextAction;
    private Map<String, Object> rawLlmResponse;
    private Instant scoredAt;
}
