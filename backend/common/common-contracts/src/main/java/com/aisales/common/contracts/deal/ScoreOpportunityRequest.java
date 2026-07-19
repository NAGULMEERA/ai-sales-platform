package com.aisales.common.contracts.deal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreOpportunityRequest {

    @Min(0)
    @Max(100)
    private Integer leadScore;

    @Min(0)
    @Max(100)
    private Integer customerScore;

    @Min(0)
    @Max(100)
    private Integer catalogMatchScore;

    @Min(0)
    @Max(100)
    private Integer aiConfidenceScore;

    @Min(0)
    @Max(100)
    private Integer conversationEngagementScore;

    @Min(0)
    @Max(100)
    private Integer activityScore;

    @Min(0)
    @Max(100)
    private Integer pipelineStageScore;

    /** Optional override; when omitted, computed from components. */
    @Min(0)
    @Max(100)
    private Integer overallScore;
}
