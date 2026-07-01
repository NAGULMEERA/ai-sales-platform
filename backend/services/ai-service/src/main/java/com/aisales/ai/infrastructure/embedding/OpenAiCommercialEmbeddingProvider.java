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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Commercial OpenAI embeddings (optional; enable when tenants upgrade from open-source).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.ai.embedding.commercial.enabled", havingValue = "true")
public class OpenAiCommercialEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingProperties properties;
    private final RestClient.Builder restClientBuilder;

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
    public List<float[]> embed(List<String> texts) {
        EmbeddingProperties.Commercial.OpenAi config = properties.getCommercial().getOpenai();
        if (!StringUtils.hasText(config.getApiKey())) {
            throw new IllegalStateException("OpenAI API key not configured for commercial embeddings");
        }

        RestClient client = restClientBuilder
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .build();

        JsonNode response = client.post()
                .uri("/embeddings")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new OpenAiEmbedRequest(config.getModel(), texts))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("data")) {
            throw new IllegalStateException("Invalid OpenAI embedding response");
        }

        List<float[]> vectors = new ArrayList<>(texts.size());
        for (JsonNode item : response.get("data")) {
            vectors.add(toFloatArray(item.get("embedding")));
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

    private record OpenAiEmbedRequest(String model, List<String> input) {
    }
}
