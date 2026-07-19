package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HybridRetrieverTest {

    @Mock private VectorRetriever vectorRetriever;
    @Mock private KeywordRetriever keywordRetriever;

    @InjectMocks private HybridRetriever hybridRetriever;

    @Test
    void shouldFuseVectorAndKeywordHitsWithRrf() {
        UUID kb = UUID.randomUUID();
        UUID shared = UUID.randomUUID();
        UUID vectorOnly = UUID.randomUUID();
        UUID keywordOnly = UUID.randomUUID();

        when(vectorRetriever.retrieve(eq(kb), eq("warranty"), anyInt())).thenReturn(List.of(
                chunk(shared, "shared warranty", 0.1),
                chunk(vectorOnly, "vector only", 0.2)));
        when(keywordRetriever.retrieve(eq(kb), eq("warranty"), anyInt())).thenReturn(List.of(
                chunk(shared, "shared warranty", 0.05),
                chunk(keywordOnly, "keyword only", 0.3)));

        List<RetrievedKnowledgeChunkDto> result = hybridRetriever.retrieve(kb, "warranty", 3);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getChunkId()).isEqualTo(shared);
        assertThat(result).extracting(RetrievedKnowledgeChunkDto::getChunkId)
                .contains(vectorOnly, keywordOnly);
    }

    @Test
    void shouldReturnVectorOnlyWhenKeywordEmpty() {
        UUID kb = UUID.randomUUID();
        UUID vectorOnly = UUID.randomUUID();
        when(vectorRetriever.retrieve(eq(kb), eq("q"), anyInt()))
                .thenReturn(List.of(chunk(vectorOnly, "v", 0.1)));
        when(keywordRetriever.retrieve(eq(kb), eq("q"), anyInt())).thenReturn(List.of());

        List<RetrievedKnowledgeChunkDto> result = hybridRetriever.retrieve(kb, "q", 5);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getChunkId()).isEqualTo(vectorOnly);
    }

    private static RetrievedKnowledgeChunkDto chunk(UUID id, String content, double distance) {
        return RetrievedKnowledgeChunkDto.builder()
                .chunkId(id)
                .documentId(UUID.randomUUID())
                .chunkIndex(0)
                .content(content)
                .distance(distance)
                .build();
    }
}
