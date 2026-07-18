package com.aisales.ai.application.rag;

import com.aisales.ai.infrastructure.configuration.EmbeddingConfiguration;
import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import com.aisales.common.observability.http.OutboundCallDiagnostics;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * TEI cross-encoder rerank ({@code POST /rerank}). Selected when
 * {@code aisales.ai.rag.reranker=TEI} and {@code aisales.ai.rag.rerank.tei.enabled=true}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aisales.ai.rag.rerank.tei.enabled", havingValue = "true")
public class TeiReranker implements Reranker {

    public static final String NAME = "TEI";

    private static final String TARGET_SERVICE = "tei-rerank";
    private static final CorrelationIdPropagationInterceptor CORRELATION_ID_INTERCEPTOR =
            new CorrelationIdPropagationInterceptor();

    private final RagProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public List<RetrievedKnowledgeChunkDto> rerank(
            String query, List<RetrievedKnowledgeChunkDto> candidates, int topK) {
        if (candidates == null || candidates.isEmpty() || topK <= 0) {
            return List.of();
        }
        if (!StringUtils.hasText(query)) {
            return candidates.stream().limit(topK).toList();
        }

        RagProperties.Rerank.Tei config = properties.getRerank().getTei();
        List<String> texts = candidates.stream()
                .map(c -> c.getContent() != null ? c.getContent() : "")
                .toList();

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
                    .uri("/rerank")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RerankRequest(query, texts, false))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            log.error("Outbound rerank call failed {} {} {} {}",
                    StructuredArguments.kv("target_service", TARGET_SERVICE),
                    StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)),
                    StructuredArguments.kv("outcome", OutboundCallDiagnostics.outcome(ex)),
                    StructuredArguments.kv("error", ex.getMessage()));
            throw ex;
        }
        log.debug("Outbound rerank call succeeded {} {}",
                StructuredArguments.kv("target_service", TARGET_SERVICE),
                StructuredArguments.kv("elapsed_ms", OutboundCallDiagnostics.elapsedMillisSince(startedAtMs)));

        if (response == null || !response.isArray()) {
            throw new IllegalStateException("Invalid rerank response from TEI");
        }

        List<Scored> scored = new ArrayList<>();
        for (JsonNode node : response) {
            int index = node.path("index").asInt(-1);
            double score = node.path("score").asDouble(0);
            if (index < 0 || index >= candidates.size()) {
                continue;
            }
            RetrievedKnowledgeChunkDto chunk = candidates.get(index);
            chunk.setScore(score);
            scored.add(new Scored(chunk, score));
        }
        scored.sort(Comparator.comparingDouble(Scored::score).reversed());
        return scored.stream().limit(topK).map(Scored::chunk).toList();
    }

    private record RerankRequest(String query, List<String> texts, boolean raw_scores) {
    }

    private record Scored(RetrievedKnowledgeChunkDto chunk, double score) {
    }
}
