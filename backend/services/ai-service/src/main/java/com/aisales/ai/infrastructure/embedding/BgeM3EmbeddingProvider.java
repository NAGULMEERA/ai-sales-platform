package com.aisales.ai.infrastructure.embedding;

import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import com.aisales.ai.infrastructure.configuration.EmbeddingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Self-hosted BAAI/bge-m3 via Text Embeddings Inference (TEI) compatible API.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.ai.embedding.open-source.enabled", havingValue = "true", matchIfMissing = true)
public class BgeM3EmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProperties properties;
    private final RestClient.Builder restClientBuilder;

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
    public List<float[]> embed(List<String> texts) {
        EmbeddingProperties.OpenSource config = properties.getOpenSource();
        RestClient client = restClientBuilder
                .baseUrl(config.getBaseUrl())
                .build();

        JsonNode response = client.post()
                .uri("/embed")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbedRequest(texts))
                .retrieve()
                .body(JsonNode.class);

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
