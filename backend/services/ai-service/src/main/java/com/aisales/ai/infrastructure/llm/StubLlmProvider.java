package com.aisales.ai.infrastructure.llm;

import com.aisales.ai.domain.llm.LlmClient;
import com.aisales.ai.domain.llm.LlmCompletionRequest;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Deterministic stub for local/dev and tests. Selected when {@code aisales.ai.llm.provider=STUB}.
 */
@Component
public class StubLlmProvider implements LlmClient {

    public static final String NAME = "STUB";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        Map<String, Object> structured = new LinkedHashMap<>();
        structured.put("purpose", request.purpose() != null ? request.purpose() : "UNKNOWN");
        structured.put("recommendation", "REVIEW");
        structured.put("summary", summarize(request.userPrompt()));
        if (request.variables() != null && !request.variables().isEmpty()) {
            structured.put("inputs", new LinkedHashMap<>(request.variables()));
        }
        structured.put("validatedByBusinessService", false);

        String raw = """
                {"recommendation":"REVIEW","summary":"%s","confidence":0.85}
                """.formatted(escape(summarize(request.userPrompt()))).trim();

        int promptTokens = estimateTokens(request.systemPrompt()) + estimateTokens(request.userPrompt());
        int completionTokens = estimateTokens(raw);

        return new LlmCompletionResult(
                NAME,
                "stub-model",
                raw,
                structured,
                0.85,
                promptTokens,
                completionTokens);
    }

    private static String summarize(String text) {
        if (text == null || text.isBlank()) {
            return "empty";
        }
        String trimmed = text.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 117) + "...";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 4);
    }
}
