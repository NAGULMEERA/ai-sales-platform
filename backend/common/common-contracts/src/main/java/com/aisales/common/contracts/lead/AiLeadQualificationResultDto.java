package com.aisales.common.contracts.lead;

import com.aisales.common.contracts.ai.QualificationResultDto;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of AI Gateway qualification. Lead state changes only when business rules accept the recommendation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiLeadQualificationResultDto {

    private LeadDto lead;

    private UUID executionId;
    private String promptCode;
    private Integer promptVersion;
    private String provider;
    private String recommendation;
    private Double confidence;
    private Integer suggestedScore;
    private boolean qualified;

    @Builder.Default
    private Map<String, String> variablesUsed = new HashMap<>();

    private String renderedUserPrompt;
    private String notes;

    /** Typed AI qualification payload (scores, risks, follow-ups, citations). */
    private QualificationResultDto qualification;
}
