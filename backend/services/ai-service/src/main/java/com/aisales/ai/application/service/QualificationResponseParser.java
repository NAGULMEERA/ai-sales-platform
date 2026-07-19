package com.aisales.ai.application.service;

import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.QualificationResultDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Parses AI Gateway structured output into a typed qualification contract.
 */
@Component
public class QualificationResponseParser {

    public QualificationResultDto parse(AiExecuteResponse ai) {
        Map<String, Object> structured = ai.getStructuredOutput() != null
                ? new LinkedHashMap<>(ai.getStructuredOutput())
                : new LinkedHashMap<>();

        Integer qualificationScore = firstInt(structured, "qualificationScore", "score", "qualification_score");
        Integer confidenceScore = firstInt(structured, "confidenceScore", "confidence_score");
        if (confidenceScore == null && ai.getConfidence() != null) {
            confidenceScore = (int) Math.round(Math.max(0, Math.min(1, ai.getConfidence())) * 100);
        }

        String recommendation = firstString(structured, "recommendation", "nextAction", "next_action");
        String reasoning = firstString(structured, "reasoning", "rationale", "explanation");
        String product = firstString(structured, "recommendedProduct", "recommended_product", "product");
        String action = firstString(
                structured, "recommendedSalesAction", "recommended_sales_action", "salesAction");

        return QualificationResultDto.builder()
                .executionId(ai.getExecutionId())
                .promptCode(ai.getPromptCode())
                .promptVersion(ai.getPromptVersion())
                .provider(ai.getProvider())
                .model(ai.getModel())
                .cacheHit(ai.isCacheHit())
                .qualificationScore(qualificationScore)
                .confidenceScore(confidenceScore)
                .confidence(ai.getConfidence())
                .recommendation(normalizeRecommendation(recommendation))
                .reasoning(reasoning)
                .recommendedProduct(product)
                .recommendedSalesAction(action)
                .riskFactors(stringList(structured, "riskFactors", "risk_factors"))
                .missingInformation(stringList(structured, "missingInformation", "missing_information"))
                .followUpQuestions(stringList(structured, "followUpQuestions", "follow_up_questions"))
                .knowledgeCitations(ai.getRetrievedChunks() != null ? ai.getRetrievedChunks() : List.of())
                .promptTokens(ai.getPromptTokens())
                .completionTokens(ai.getCompletionTokens())
                .rawStructuredOutput(structured)
                .build();
    }

    private static String normalizeRecommendation(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Integer firstInt(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            Integer parsed = toInt(value);
            if (parsed != null) {
                return Math.max(0, Math.min(100, parsed));
            }
        }
        return null;
    }

    private static String firstString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object item : list) {
                    if (item != null && StringUtils.hasText(String.valueOf(item))) {
                        out.add(String.valueOf(item).trim());
                    }
                }
                return out;
            }
            if (value instanceof String text && StringUtils.hasText(text)) {
                return List.of(text.trim());
            }
        }
        return new ArrayList<>();
    }

    private static Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
