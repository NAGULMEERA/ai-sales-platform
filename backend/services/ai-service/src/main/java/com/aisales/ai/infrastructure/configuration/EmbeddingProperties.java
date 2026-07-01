package com.aisales.ai.infrastructure.configuration;

import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aisales.ai.embedding")
public class EmbeddingProperties {

    /** Default when tenant has no override: OPEN_SOURCE (BGE-M3). */
    private EmbeddingProviderKind defaultProviderKind = EmbeddingProviderKind.OPEN_SOURCE;

    private String defaultModel = "BAAI/bge-m3";

    private int defaultDimension = 1024;

    private OpenSource openSource = new OpenSource();

    private Commercial commercial = new Commercial();

    @Data
    public static class OpenSource {
        private boolean enabled = true;
        /** Text Embeddings Inference (TEI) or compatible endpoint. */
        private String baseUrl = "http://localhost:8099";
        private String model = "BAAI/bge-m3";
        private int dimension = 1024;
        private int connectTimeoutMs = 5000;
        private int readTimeoutMs = 30000;
    }

    @Data
    public static class Commercial {
        private boolean enabled = false;
        private OpenAi openai = new OpenAi();

        @Data
        public static class OpenAi {
            private String apiKey = "";
            private String baseUrl = "https://api.openai.com/v1";
            private String model = "text-embedding-3-small";
            private int dimension = 1536;
        }
    }
}
