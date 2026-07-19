package com.aisales.common.contracts.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Strongly typed AI lead-qualification output. Never treat raw LLM strings as the contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualificationResultDto {

    private UUID executionId;
    private String promptCode;
    private Integer promptVersion;
    private String provider;
    private String model;
    private boolean cacheHit;

    private Integer qualificationScore;
    private Integer confidenceScore;
    private Double confidence;
    private String recommendation;
    private String reasoning;
    private String recommendedProduct;
    private String recommendedSalesAction;

    @Builder.Default
    private List<String> riskFactors = new ArrayList<>();

    @Builder.Default
    private List<String> missingInformation = new ArrayList<>();

    @Builder.Default
    private List<String> followUpQuestions = new ArrayList<>();

    @Builder.Default
    private List<RetrievedKnowledgeChunkDto> knowledgeCitations = new ArrayList<>();

    private Integer promptTokens;
    private Integer completionTokens;

    @Builder.Default
    private Map<String, Object> rawStructuredOutput = new HashMap<>();
}
