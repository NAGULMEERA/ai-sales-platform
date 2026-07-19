package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Tag("concurrency")
@ExtendWith(MockitoExtension.class)
class HybridRetrieverConcurrencyTest {

    @Mock private VectorRetriever vectorRetriever;
    @Mock private KeywordRetriever keywordRetriever;

    @InjectMocks private HybridRetriever hybridRetriever;

    @Test
    void shouldReturnStableFusionUnderConcurrentRetrieve() throws Exception {
        UUID kb = UUID.randomUUID();
        UUID shared = UUID.randomUUID();
        UUID vectorOnly = UUID.randomUUID();

        when(vectorRetriever.retrieve(eq(kb), eq("warranty"), anyInt())).thenReturn(List.of(
                chunk(shared, "shared", 0.1),
                chunk(vectorOnly, "vector", 0.2)));
        when(keywordRetriever.retrieve(eq(kb), eq("warranty"), anyInt()))
                .thenReturn(List.of(chunk(shared, "shared", 0.05)));

        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<List<RetrievedKnowledgeChunkDto>>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                tasks.add(() -> hybridRetriever.retrieve(kb, "warranty", 5));
            }
            List<Future<List<RetrievedKnowledgeChunkDto>>> futures = pool.invokeAll(tasks, 10, TimeUnit.SECONDS);

            for (Future<List<RetrievedKnowledgeChunkDto>> future : futures) {
                List<RetrievedKnowledgeChunkDto> result = future.get(5, TimeUnit.SECONDS);
                assertThat(result).isNotEmpty();
                assertThat(result.get(0).getChunkId()).isEqualTo(shared);
                assertThat(result).extracting(RetrievedKnowledgeChunkDto::getChunkId).contains(vectorOnly);
            }
        } finally {
            pool.shutdownNow();
        }
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
