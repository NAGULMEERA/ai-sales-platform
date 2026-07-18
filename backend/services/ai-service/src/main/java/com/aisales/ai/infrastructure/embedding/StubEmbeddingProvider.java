package com.aisales.ai.infrastructure.embedding;

import com.aisales.ai.domain.embedding.EmbeddingProvider;
import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Deterministic local embeddings for tests / offline RAG. Selected when
 * {@code aisales.ai.embedding.provider=STUB}.
 */
@Component
@ConditionalOnProperty(name = "aisales.ai.embedding.stub.enabled", havingValue = "true")
public class StubEmbeddingProvider implements EmbeddingProvider {

    public static final String NAME = "STUB";
    public static final String MODEL = "stub-embedding-1024";
    private static final int DIMENSION = 1024;

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
        return MODEL;
    }

    @Override
    public int dimension() {
        return DIMENSION;
    }

    @Override
    public boolean supports(String modelName) {
        return MODEL.equals(modelName);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(hashToUnitVector(text != null ? text : ""));
        }
        return vectors;
    }

    private static float[] hashToUnitVector(String text) {
        float[] vector = new float[DIMENSION];
        CRC32 crc = new CRC32();
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        for (int i = 0; i < DIMENSION; i++) {
            crc.reset();
            crc.update(bytes);
            crc.update(i);
            // Pseudo-random but deterministic components in [-1, 1]
            long v = crc.getValue();
            vector[i] = ((v % 10_000) / 5_000.0f) - 1.0f;
        }
        double norm = 0;
        for (float f : vector) {
            norm += f * f;
        }
        norm = Math.sqrt(norm);
        if (norm == 0) {
            vector[0] = 1f;
            return vector;
        }
        for (int i = 0; i < DIMENSION; i++) {
            vector[i] = (float) (vector[i] / norm);
        }
        return vector;
    }
}
