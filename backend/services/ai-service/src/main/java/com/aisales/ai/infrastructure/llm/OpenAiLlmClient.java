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
import java.util.ArrayList;
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
 * OpenAI Chat Completions client. Enable with {@code aisales.ai.llm.openai.enabled=true}
 * and select via {@code aisales.ai.llm.provider=OPENAI}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.ai.llm.openai.enabled", havingValue = "true")
public class OpenAiLlmClient implements LlmClient {

    public static final String NAME = "OPENAI";
    private static final String TARGET_SERVICE = "openai-chat";

    private final LlmProperties properties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    @CircuitBreaker(name = "openAiLlm")
    @Retry(name = "openAiLlm")
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        LlmProperties.OpenAi config = properties.getOpenai();
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new BusinessException(
                    ErrorCode.AI_UNAVAILABLE, "OpenAI API key not configured (aisales.ai.llm.openai.api-key)");
        }

        RestClient client = restClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(EmbeddingConfiguration.timeoutRequestFactory(
                        config.getConnectTimeoutMs(), config.getReadTimeoutMs()))
                .build();

        Map<String, Object> body = buildRequestBody(request, config);
        long startedAtMs = System.currentTimeMillis();
        JsonNode response;
        try {
            response = client.post()
                    .uri("/chat/completions")
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
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "OpenAI chat completion failed: " + ex.getMessage());
        }
        log.debug("Outbound LLM call succeeded {} {}",
                StructuredArguments.kv("target_service", TARGET_SERVICE),
                StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)));

        return mapResponse(response, config.getModel());
    }

    private Map<String, Object> buildRequestBody(LlmCompletionRequest request, LlmProperties.OpenAi config) {
        List<Map<String, String>> messages = new ArrayList<>();
        String system = request.systemPrompt();
        if (StringUtils.hasText(request.expectedOutputHint())) {
            system = (StringUtils.hasText(system) ? system + "\n\n" : "")
                    + "Expected output: " + request.expectedOutputHint().trim();
        }
        if (StringUtils.hasText(system)) {
            messages.add(Map.of("role", "system", "content", system));
        }
        messages.add(Map.of(
                "role",
                "user",
                "content",
                request.userPrompt() != null ? request.userPrompt() : ""));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModel());
        body.put("messages", messages);
        if (config.getTemperature() != null) {
            body.put("temperature", config.getTemperature());
        }
        if (config.getMaxTokens() != null) {
            body.put("max_tokens", config.getMaxTokens());
        }
        if (config.isJsonObjectResponse() && StringUtils.hasText(request.expectedOutputHint())) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        return body;
    }

    LlmCompletionResult mapResponse(JsonNode response, String fallbackModel) {
        if (response == null || !response.has("choices") || !response.get("choices").isArray()
                || response.get("choices").isEmpty()) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "Invalid OpenAI chat completion response");
        }

        JsonNode choice = response.get("choices").get(0);
        JsonNode message = choice.path("message");
        String rawText = message.path("content").asText("");
        String model = response.has("model") ? response.get("model").asText(fallbackModel) : fallbackModel;

        Integer promptTokens = null;
        Integer completionTokens = null;
        if (response.has("usage")) {
            JsonNode usage = response.get("usage");
            if (usage.has("prompt_tokens")) {
                promptTokens = usage.get("prompt_tokens").asInt();
            }
            if (usage.has("completion_tokens")) {
                completionTokens = usage.get("completion_tokens").asInt();
            }
        }

        Map<String, Object> structured = parseStructured(rawText);
        Double confidence = extractConfidence(structured);

        return new LlmCompletionResult(
                NAME, model, rawText, structured, confidence, promptTokens, completionTokens);
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
