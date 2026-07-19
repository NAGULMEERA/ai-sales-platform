package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.QualificationResultDto;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QualificationResponseParserTest {

    private final QualificationResponseParser parser = new QualificationResponseParser();

    @Test
    void shouldParseStructuredQualificationFields() {
        QualificationResultDto result = parser.parse(AiExecuteResponse.builder()
                .executionId(UUID.randomUUID())
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .promptVersion(1)
                .provider("STUB")
                .model("stub-model")
                .confidence(0.91)
                .cacheHit(false)
                .structuredOutput(Map.of(
                        "qualificationScore", 84,
                        "confidenceScore", 91,
                        "recommendation", "qualify",
                        "reasoning", "Strong budget and timeline",
                        "recommendedProduct", "3BHK",
                        "recommendedSalesAction", "Schedule site visit",
                        "riskFactors", List.of("price sensitivity"),
                        "missingInformation", List.of("preferred floor"),
                        "followUpQuestions", List.of("When can you visit?")))
                .build());

        assertThat(result.getQualificationScore()).isEqualTo(84);
        assertThat(result.getConfidenceScore()).isEqualTo(91);
        assertThat(result.getRecommendation()).isEqualTo("QUALIFY");
        assertThat(result.getRecommendedProduct()).isEqualTo("3BHK");
        assertThat(result.getRiskFactors()).containsExactly("price sensitivity");
        assertThat(result.getFollowUpQuestions()).containsExactly("When can you visit?");
    }

    @Test
    void shouldFallbackToLegacyScoreKey() {
        QualificationResultDto result = parser.parse(AiExecuteResponse.builder()
                .structuredOutput(Map.of("recommendation", "REVIEW", "score", 55))
                .confidence(0.7)
                .build());

        assertThat(result.getQualificationScore()).isEqualTo(55);
        assertThat(result.getRecommendation()).isEqualTo("REVIEW");
        assertThat(result.getConfidenceScore()).isEqualTo(70);
    }
}
