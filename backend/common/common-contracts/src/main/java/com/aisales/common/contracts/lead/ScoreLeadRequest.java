package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Component scores are stored individually; {@link #score} is the derived overall
 * (explicit or averaged from components when omitted).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreLeadRequest {

    @Min(0)
    @Max(100)
    private Integer score;

    @Size(max = 50)
    private String scoreType;

    @Size(max = 2000)
    private String explanation;

    @Min(0)
    @Max(100)
    private Integer budgetScore;

    @Min(0)
    @Max(100)
    private Integer timelineScore;

    @Min(0)
    @Max(100)
    private Integer locationScore;

    @Min(0)
    @Max(100)
    private Integer engagementScore;

    @Min(0)
    @Max(100)
    private Integer aiConfidenceScore;
}
