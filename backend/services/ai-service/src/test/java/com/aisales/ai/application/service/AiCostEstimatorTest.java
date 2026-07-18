package com.aisales.ai.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.ai.infrastructure.configuration.AiCostProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiCostEstimatorTest {

    private AiCostEstimator estimator;

    @BeforeEach
    void setUp() {
        AiCostProperties properties = new AiCostProperties();
        properties.setEnabled(true);
        AiCostProperties.ModelRate rate = new AiCostProperties.ModelRate();
        rate.setPromptPer1k(new BigDecimal("0.001"));
        rate.setCompletionPer1k(new BigDecimal("0.002"));
        rate.setEmbeddingPer1k(new BigDecimal("0.0001"));
        properties.getModels().put("gemini-2.0-flash", rate);
        estimator = new AiCostEstimator(properties);
    }

    @Test
    void shouldEstimateLlmAndEmbedding() {
        assertThat(estimator.estimateLlm("gemini-2.0-flash", 1000, 1000))
                .isEqualByComparingTo("0.00300000");
        assertThat(estimator.estimateEmbedding("gemini-2.0-flash", 2000))
                .isEqualByComparingTo("0.00020000");
    }

    @Test
    void shouldReturnZeroWhenModelUnknown() {
        assertThat(estimator.estimateLlm("unknown-model", 1000, 0))
                .isEqualByComparingTo("0.00000000");
    }
}
