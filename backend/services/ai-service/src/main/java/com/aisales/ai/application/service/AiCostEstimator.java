package com.aisales.ai.application.service;

import com.aisales.ai.infrastructure.configuration.AiCostProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AiCostEstimator {

    private final AiCostProperties properties;

    public BigDecimal estimateLlm(String model, int promptTokens, int completionTokens) {
        if (!properties.isEnabled()) {
            return null;
        }
        AiCostProperties.ModelRate rate = rateFor(model);
        if (rate == null) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return per1k(promptTokens, rate.getPromptPer1k())
                .add(per1k(completionTokens, rate.getCompletionPer1k()))
                .setScale(8, RoundingMode.HALF_UP);
    }

    public BigDecimal estimateEmbedding(String model, int embeddingTokens) {
        if (!properties.isEnabled()) {
            return null;
        }
        AiCostProperties.ModelRate rate = rateFor(model);
        if (rate == null) {
            return BigDecimal.ZERO.setScale(8, RoundingMode.HALF_UP);
        }
        return per1k(embeddingTokens, rate.getEmbeddingPer1k()).setScale(8, RoundingMode.HALF_UP);
    }

    private AiCostProperties.ModelRate rateFor(String model) {
        if (!StringUtils.hasText(model) || properties.getModels() == null) {
            return null;
        }
        AiCostProperties.ModelRate exact = properties.getModels().get(model);
        if (exact != null) {
            return exact;
        }
        // Allow prefix keys (e.g. gemini-2.0-flash matches gemini-2.0-flash-001)
        return properties.getModels().entrySet().stream()
                .filter(e -> model.startsWith(e.getKey()) || e.getKey().startsWith(model))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static BigDecimal per1k(int tokens, BigDecimal ratePer1k) {
        if (tokens <= 0 || ratePer1k == null) {
            return BigDecimal.ZERO;
        }
        return ratePer1k
                .multiply(BigDecimal.valueOf(tokens))
                .divide(BigDecimal.valueOf(1000), 12, RoundingMode.HALF_UP);
    }
}
