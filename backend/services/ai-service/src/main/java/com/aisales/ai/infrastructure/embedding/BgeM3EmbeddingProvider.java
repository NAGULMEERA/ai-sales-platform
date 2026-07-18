package com.aisales.ai.infrastructure.embedding;

import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingConfiguration;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import com.aisales.common.observability.http.OutboundCallDiagnostics;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-hosted BAAI/bge-m3 via Text Embeddings Inference (TEI) compatible API.
 * Selected when {@code aisales.ai.embedding.provider=TEI}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.ai.embedding.open-source.enabled", havingValue = "true", matchIfMissing = true)
public class BgeM3EmbeddingProvider implements EmbeddingProvider {

    public static final String NAME = "TEI";

    private static final String TARGET_SERVICE = "bge-m3-embedding";

    // TEI runs on the same internal network as this service (unlike the commercial OpenAI
    // provider), so propagating our correlation_id is safe and lets the two services' logs be
    // joined on the same id (Rule 08: "never lose traceability").
    private static final CorrelationIdPropagationInterceptor CORRELATION_ID_INTERCEPTOR =
            new CorrelationIdPropagationInterceptor();

    private final EmbeddingProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public EmbeddingProviderKind kind() {
        return EmbeddingProviderKind.OPEN_SOURCE;
    }

    @Override
    public String modelName() {
        return properties.getOpenSource().getModel();
    }

    @Override
    public int dimension() {
        return properties.getOpenSource().getDimension();
    }

    @Override
    public boolean supports(String modelName) {
        if (modelName == null) {
            return true;
        }
        return modelName().equalsIgnoreCase(modelName)
                || "bge-m3".equalsIgnoreCase(modelName)
                || "BAAI/bge-m3".equalsIgnoreCase(modelName);
    }

    @Override
    @Retry(name = "bgeM3Embedding")
    public List<float[]> embed(List<String> texts) {
        EmbeddingProperties.OpenSource config = properties.getOpenSource();
        RestClient client = restClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .requestFactory(EmbeddingConfiguration.timeoutRequestFactory(
                        config.getConnectTimeoutMs(), config.getReadTimeoutMs()))
                .requestInterceptor(CORRELATION_ID_INTERCEPTOR)
                .build();

        long startedAtMs = System.currentTimeMillis();
        JsonNode response;
        try {
            response = client.post()
                    .uri("/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EmbedRequest(texts))
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

        if (response == null || !response.isArray()) {
            throw new IllegalStateException("Invalid embedding response from open-source provider");
        }

        List<float[]> vectors = new ArrayList<>(texts.size());
        for (JsonNode node : response) {
            vectors.add(toFloatArray(node));
        }
        return vectors;
    }

    private static float[] toFloatArray(JsonNode node) {
        float[] values = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            values[i] = (float) node.get(i).asDouble();
        }
        return values;
    }

    private record EmbedRequest(List<String> inputs) {
    }
}
