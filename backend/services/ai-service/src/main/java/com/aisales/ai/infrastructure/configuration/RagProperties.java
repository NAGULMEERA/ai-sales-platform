package com.aisales.ai.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG retrieval + chunking settings.
 * Switch chunker with {@code aisales.ai.rag.chunker}: {@code CHAR} | {@code TOKEN}.
 */
@Data
@ConfigurationProperties(prefix = "aisales.ai.rag")
public class RagProperties {

    private int defaultTopK = 5;

    private double maxCosineDistance = 0.45;

    /**
     * Active chunker key resolved by {@code TextChunker}.
     * Plug/flag: {@code CHAR} | {@code TOKEN}.
     */
    private String chunker = "TOKEN";

    /**
     * Document extractor for media ingest.
     * Plug/flag: {@code AUTO} | {@code TEXT} | {@code PDF}.
     */
    private String extractor = "AUTO";

    private CharChunker charWindow = new CharChunker();

    private TokenChunker tokenWindow = new TokenChunker();

    /**
     * Active retriever key resolved by {@code RetrieverRegistry}.
     * Plug/flag: {@code VECTOR} | {@code KEYWORD} | {@code HYBRID}.
     */
    private String retriever = "VECTOR";

    /**
     * Active reranker key resolved by {@code RerankerRegistry}.
     * Plug/flag: {@code NONE} | {@code STUB} | {@code TEI}.
     */
    private String reranker = "STUB";

    private Rerank rerank = new Rerank();

    @Data
    public static class CharChunker {
        private int chunkSize = 800;
        private int overlap = 120;
    }

    @Data
    public static class TokenChunker {
        /** Approximate word/punctuation tokens per chunk. */
        private int chunkSize = 200;
        private int overlap = 40;
    }

    @Data
    public static class Rerank {
        /** Vector search over-fetch factor before rerank (final size remains top-k). */
        private int candidateMultiplier = 3;
        private int maxCandidates = 30;
        private Tei tei = new Tei();

        @Data
        public static class Tei {
            /** When false, TEI reranker bean is not registered. */
            private boolean enabled = false;
            /** Separate TEI process hosting a cross-encoder (e.g. BAAI/bge-reranker-base). */
            private String baseUrl = "http://localhost:8098";
            private int connectTimeoutMs = 2000;
            private int readTimeoutMs = 30000;
        }
    }
}
