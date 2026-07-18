package com.aisales.ai.domain.llm;

import java.util.Map;

public record LlmCompletionRequest(
        String systemPrompt,
        String userPrompt,
        String purpose,
        String expectedOutputHint,
        Map<String, String> variables
) {
}
