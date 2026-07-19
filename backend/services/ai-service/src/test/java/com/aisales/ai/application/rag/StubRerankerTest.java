package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class StubRerankerTest {

    private final StubReranker reranker = new StubReranker();

    @Test
    void shouldPreferLexicalMatchOverDistantVectorHit() {
        RetrievedKnowledgeChunkDto lexical = chunk("Warranty covers three years for all models.", 0.4);
        RetrievedKnowledgeChunkDto distant = chunk("Office hours are 9 to 5 on weekdays.", 0.05);

        List<RetrievedKnowledgeChunkDto> ranked =
                reranker.rerank("warranty years", List.of(distant, lexical), 2);

        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).getContent()).contains("Warranty");
        assertThat(ranked.get(0).getScore()).isGreaterThan(ranked.get(1).getScore());
    }

    @Test
    void shouldRespectTopK() {
        List<RetrievedKnowledgeChunkDto> ranked = reranker.rerank(
                "a",
                List.of(chunk("a", 0.1), chunk("b", 0.2), chunk("c", 0.3)),
                1);
        assertThat(ranked).hasSize(1);
    }

    private static RetrievedKnowledgeChunkDto chunk(String content, double distance) {
        return RetrievedKnowledgeChunkDto.builder()
                .chunkId(UUID.randomUUID())
                .documentId(UUID.randomUUID())
                .chunkIndex(0)
                .content(content)
                .distance(distance)
                .build();
    }
}
