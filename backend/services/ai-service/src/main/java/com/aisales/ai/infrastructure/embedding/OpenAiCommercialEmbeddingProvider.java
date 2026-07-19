package com.aisales.ai.infrastructure.embedding;

import com.aisales.ai.domain.embedding.EmbeddingBatchResult;
import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingConfiguration;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.aisales.common.observability.http.OutboundCallDiagnostics;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * Commercial OpenAI embeddings. Selected when {@code aisales.ai.embedding.provider=OPENAI}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.ai.embedding.commercial.enabled", havingValue = "true")
public class OpenAiCommercialEmbeddingProvider implements EmbeddingProvider {

    public static final String NAME = "OPENAI";

    private static final String TARGET_SERVICE = "openai-embeddings";

    private final EmbeddingProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EmbeddingProviderKind kind() {
        return EmbeddingProviderKind.COMMERCIAL;
    }

    @Override
    public String modelName() {
        return properties.getCommercial().getOpenai().getModel();
    }

    @Override
    public int dimension() {
        return properties.getCommercial().getOpenai().getDimension();
    }

    @Override
    public boolean supports(String modelName) {
        if (modelName == null) {
            return true;
        }
        return modelName.startsWith("text-embedding-");
    }

    @Override
    @CircuitBreaker(name = "openAiEmbedding")
    @Retry(name = "openAiEmbedding")
    public List<float[]> embed(List<String> texts) {
        return embedWithUsage(texts).vectors();
    }

    @Override
    @CircuitBreaker(name = "openAiEmbedding")
    @Retry(name = "openAiEmbedding")
    public EmbeddingBatchResult embedWithUsage(List<String> texts) {
        EmbeddingProperties.Commercial.OpenAi config = properties.getCommercial().getOpenai();
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new IllegalStateException("OpenAI API key not configured for commercial embeddings");
        }

        RestClient client = restClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .requestFactory(EmbeddingConfiguration.timeoutRequestFactory(
                        config.getConnectTimeoutMs(), config.getReadTimeoutMs()))
                .build();

        long startedAtMs = System.currentTimeMillis();
        JsonNode response;
        try {
            response = client.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OpenAiEmbedRequest(config.getModel(), texts))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.error("Outbound embedding call failed {} {} {} {}",
                    StructuredArguments.kv("target_service", TARGET_SERVICE),
                    StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)),
                    StructuredArguments.kv("outcome", OutboundCallDiagnostics.outcome(ex)),
                    StructuredArguments.kv("error", ex.getMessage()));
            throw ex;
        }
        log.debug("Outbound embedding call succeeded {} {}",
                StructuredArguments.kv("target_service", TARGET_SERVICE),
                StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)));

        if (response == null || !response.has("data")) {
            throw new IllegalStateException("Invalid OpenAI embedding response");
        }

        List<float[]> vectors = new ArrayList<>(texts.size());
        for (JsonNode item : response.get("data")) {
            vectors.add(toFloatArray(item.get("embedding")));
        }
        Integer promptTokens = null;
        if (response.has("usage") && response.get("usage").has("prompt_tokens")) {
            promptTokens = response.get("usage").get("prompt_tokens").asInt();
        } else if (response.has("usage") && response.get("usage").has("total_tokens")) {
            promptTokens = response.get("usage").get("total_tokens").asInt();
        }
        return new EmbeddingBatchResult(vectors, promptTokens);
    }

    private static float[] toFloatArray(JsonNode node) {
        float[] values = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            values[i] = (float) node.get(i).asDouble();
        }
        return values;
    }

    private record OpenAiEmbedRequest(String model, List<String> input) {
    }
}
