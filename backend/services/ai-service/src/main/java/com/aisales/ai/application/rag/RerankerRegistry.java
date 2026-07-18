package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Routes RAG rerank to the configured strategy.
 * Switch with {@code aisales.ai.rag.reranker} only ({@code NONE} | {@code STUB} | {@code TEI}).
 */
@Component
@RequiredArgsConstructor
public class RerankerRegistry {

    private final List<Reranker> rerankers;
    private final RagProperties properties;

    public Reranker resolveDefault() {
        return resolveByName(properties.getReranker());
    }

    public Reranker resolveByName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException(ErrorCode.AI_UNAVAILABLE, "aisales.ai.rag.reranker is not set");
        }
        String key = name.trim().toUpperCase(Locale.ROOT);
        return rerankers.stream()
                .filter(r -> key.equals(r.name().toUpperCase(Locale.ROOT)))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.AI_UNAVAILABLE,
                        "No reranker registered for aisales.ai.rag.reranker="
                                + key
                                + ". Available: "
                                + rerankers.stream()
                                        .map(Reranker::name)
                                        .sorted()
                                        .collect(Collectors.joining(", "))
                                + ". For TEI also set aisales.ai.rag.rerank.tei.enabled=true."));
    }
}
