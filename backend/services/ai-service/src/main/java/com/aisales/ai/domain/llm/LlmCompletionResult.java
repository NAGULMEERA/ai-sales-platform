package com.aisales.ai.domain.llm;

import java.util.Map;

public record LlmCompletionResult(
        String provider,
        String model,
        String rawText,
        Map<String, Object> structuredOutput,
        Double confidence,
        Integer promptTokens,
        Integer completionTokens
) {
}
