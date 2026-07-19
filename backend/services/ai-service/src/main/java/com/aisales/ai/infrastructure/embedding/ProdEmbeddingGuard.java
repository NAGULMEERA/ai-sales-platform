package com.aisales.ai.infrastructure.embedding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Fail-fast in production when stub embeddings are still selected.
 * RAG quality requires TEI or a commercial provider in prod (Rule 09).
 */
@Component
@Profile("prod")
public class ProdEmbeddingGuard implements ApplicationRunner {

    private final String provider;

    public ProdEmbeddingGuard(@Value("${aisales.ai.embedding.provider:STUB}") String provider) {
        this.provider = provider;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (provider == null || "STUB".equalsIgnoreCase(provider.trim())) {
            throw new IllegalStateException(
                    "aisales.ai.embedding.provider=STUB is forbidden when spring.profiles.active includes prod. "
                            + "Set aisales.ai.embedding.provider=TEI or OPENAI "
                            + "(and enable open-source / commercial accordingly) before deploying.");
        }
    }
}
