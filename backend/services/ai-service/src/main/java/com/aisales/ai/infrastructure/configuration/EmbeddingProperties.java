package com.aisales.ai.infrastructure.configuration;

import com.aisales.ai.domain.embedding.EmbeddingProviderKind;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aisales.ai.embedding")
public class EmbeddingProperties {

    /**
     * Active provider key resolved by {@code EmbeddingProviderRegistry}.
     * Plug/flag switch: {@code STUB} | {@code TEI} | {@code OPENAI}.
     * Matching bean must be enabled ({@code stub.enabled} / {@code open-source.enabled} /
     * {@code commercial.enabled}).
     */
    private String provider = "STUB";

    /**
     * Legacy kind hint for request overrides that omit an explicit provider key.
     * Prefer {@link #provider} for the platform default.
     */
    private EmbeddingProviderKind defaultProviderKind = EmbeddingProviderKind.OPEN_SOURCE;

    private String defaultModel = "stub-embedding-1024";

    private int defaultDimension = 1024;

    private Stub stub = new Stub();

    private OpenSource openSource = new OpenSource();

    private Commercial commercial = new Commercial();

    @Data
    public static class Stub {
        /** When false, stub bean is not registered. */
        private boolean enabled = false;
    }

    @Data
    public static class OpenSource {
        private boolean enabled = true;
        /** Text Embeddings Inference (TEI) or compatible endpoint. */
        private String baseUrl = "http://localhost:8099";
        private String model = "BAAI/bge-m3";
        private int dimension = 1024;
        /**
         * TEI runs on the same internal network, so the TCP handshake should complete almost
         * instantly; 2s already gives generous headroom over a genuinely healthy dependency.
         */
        private int connectTimeoutMs = 2000;
        /**
         * Unlike the connect timeout, this is deliberately generous: model inference time (not
         * network latency) dominates here, and CPU-hosted batch embedding can legitimately take
         * several seconds to tens of seconds depending on batch size and hardware.
         */
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
            /** Public-internet call; kept short so a DNS/TLS handshake issue fails fast. */
            private int connectTimeoutMs = 3000;
            /** Batch embedding requests over the internet; generous but bounded. */
            private int readTimeoutMs = 20000;
        }
    }
}
