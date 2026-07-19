package com.aisales.ai.infrastructure.llm;

import com.aisales.ai.domain.llm.LlmClient;
import com.aisales.ai.domain.llm.LlmCompletionRequest;
import com.aisales.ai.domain.llm.LlmCompletionResult;
import com.aisales.ai.infrastructure.configuration.EmbeddingConfiguration;
import com.aisales.ai.infrastructure.configuration.LlmProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.observability.http.OutboundCallDiagnostics;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Google Gemini generateContent client.
 * Enable with {@code aisales.ai.llm.gemini.enabled=true} and select via
 * {@code aisales.ai.llm.provider=GEMINI}. Switch providers with that flag only
 * (OPENAI / STUB / GEMINI) once the target client bean is enabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.ai.llm.gemini.enabled", havingValue = "true")
public class GeminiLlmClient implements LlmClient {

    public static final String NAME = "GEMINI";
    private static final String TARGET_SERVICE = "gemini-generate";

    private final LlmProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    @CircuitBreaker(name = "geminiLlm")
    @Retry(name = "geminiLlm")
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        LlmProperties.Gemini config = properties.getGemini();
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new BusinessException(
                    ErrorCode.AI_UNAVAILABLE, "Gemini API key not configured (aisales.ai.llm.gemini.api-key)");
        }

        RestClient client = restClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("x-goog-api-key", config.getApiKey())
                .requestFactory(EmbeddingConfiguration.timeoutRequestFactory(
                        config.getConnectTimeoutMs(), config.getReadTimeoutMs()))
                .build();

        Map<String, Object> body = buildRequestBody(request, config);
        String uri = "/models/" + config.getModel() + ":generateContent";
        long startedAtMs = System.currentTimeMillis();
        JsonNode response;
        try {
            response = client.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.error("Outbound LLM call failed {} {} {} {}",
                    StructuredArguments.kv("target_service", TARGET_SERVICE),
                    StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)),
                    StructuredArguments.kv("outcome", OutboundCallDiagnostics.outcome(ex)),
                    StructuredArguments.kv("error", ex.getMessage()));
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "Gemini generateContent failed: " + ex.getMessage());
        }
        log.debug("Outbound LLM call succeeded {} {}",
                StructuredArguments.kv("target_service", TARGET_SERVICE),
                StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)));

        return mapResponse(response, config.getModel());
    }

    private Map<String, Object> buildRequestBody(LlmCompletionRequest request, LlmProperties.Gemini config) {
        Map<String, Object> body = new LinkedHashMap<>();

        String system = request.systemPrompt();
        if (StringUtils.hasText(request.expectedOutputHint())) {
            system = (StringUtils.hasText(system) ? system + "\n\n" : "")
                    + "Expected output: " + request.expectedOutputHint().trim();
        }
        if (StringUtils.hasText(system)) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", system))));
        }

        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of(
                        "text", request.userPrompt() != null ? request.userPrompt() : "")))));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        if (config.getTemperature() != null) {
            generationConfig.put("temperature", config.getTemperature());
        }
        if (config.getMaxOutputTokens() != null) {
            generationConfig.put("maxOutputTokens", config.getMaxOutputTokens());
        }
        if (config.isJsonObjectResponse() && StringUtils.hasText(request.expectedOutputHint())) {
            generationConfig.put("responseMimeType", "application/json");
        }
        if (!generationConfig.isEmpty()) {
            body.put("generationConfig", generationConfig);
        }
        return body;
    }

    LlmCompletionResult mapResponse(JsonNode response, String fallbackModel) {
        if (response == null || !response.has("candidates") || !response.get("candidates").isArray()
                || response.get("candidates").isEmpty()) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "Invalid Gemini generateContent response");
        }

        JsonNode candidate = response.get("candidates").get(0);
        String rawText = extractText(candidate.path("content"));
        String model = response.has("modelVersion")
                ? response.get("modelVersion").asText(fallbackModel)
                : fallbackModel;

        Integer promptTokens = null;
        Integer completionTokens = null;
        if (response.has("usageMetadata")) {
            JsonNode usage = response.get("usageMetadata");
            if (usage.has("promptTokenCount")) {
                promptTokens = usage.get("promptTokenCount").asInt();
            }
            if (usage.has("candidatesTokenCount")) {
                completionTokens = usage.get("candidatesTokenCount").asInt();
            }
        }

        Map<String, Object> structured = parseStructured(rawText);
        Double confidence = extractConfidence(structured);

        return new LlmCompletionResult(
                NAME, model, rawText, structured, confidence, promptTokens, completionTokens);
    }

    private static String extractText(JsonNode content) {
        if (content == null || !content.has("parts") || !content.get("parts").isArray()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : content.get("parts")) {
            if (part.has("text")) {
                sb.append(part.get("text").asText(""));
            }
        }
        return sb.toString();
    }

    private Map<String, Object> parseStructured(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return Map.of();
        }
        String trimmed = rawText.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return Map.of();
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node != null && node.isObject()) {
                return objectMapper.convertValue(node, new TypeReference<LinkedHashMap<String, Object>>() {});
            }
        } catch (Exception ignored) {
            // Non-JSON completions remain raw-text only; business services validate.
        }
        return Map.of();
    }

    private static Double extractConfidence(Map<String, Object> structured) {
        if (structured == null || !structured.containsKey("confidence")) {
            return null;
        }
        Object value = structured.get("confidence");
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
