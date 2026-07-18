package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
public class RecordLeadQualityScoreRequest {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer overallScore;

    private String budgetFit;
    private String timeline;
    private String decisionMaker;
    private String competitorAwareness;
    private List<String> objections;
    private String suggestedProduct;
    private String nextAction;
    private Map<String, Object> rawLlmResponse;
}
