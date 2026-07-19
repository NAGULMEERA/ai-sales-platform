package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.contracts.ai.RetrievedKnowledgeChunkDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class KnowledgeContextAssemblerTest {

    private final KnowledgeContextAssembler assembler = new KnowledgeContextAssembler();

    @Test
    void shouldAssembleNumberedChunksAsUntrustedData() {
        String text = assembler.assemble(List.of(
                RetrievedKnowledgeChunkDto.builder()
                        .chunkId(UUID.randomUUID())
                        .content("First fact")
                        .build(),
                RetrievedKnowledgeChunkDto.builder()
                        .chunkId(UUID.randomUUID())
                        .content("Second fact")
                        .build()));

        assertThat(text)
                .contains("untrusted document data")
                .contains("<<<KNOWLEDGE_DATA>>>")
                .contains("[1] First fact")
                .contains("[2] Second fact")
                .contains("<<<END_KNOWLEDGE_DATA>>>");
    }

    @Test
    void shouldReturnEmptyForNoChunks() {
        assertThat(assembler.assemble(List.of())).isEmpty();
        assertThat(assembler.assemble(null)).isEmpty();
    }
}
