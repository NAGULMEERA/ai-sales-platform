package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class TokenWindowChunkerTest {

    private final RagProperties properties = new RagProperties();
    private final TokenWindowChunker chunker = new TokenWindowChunker(properties);

    @Test
    void shouldReturnSingleChunkWhenTokensFit() {
        assertThat(chunker.chunk("short warranty text", 10, 2)).containsExactly("short warranty text");
    }

    @Test
    void shouldPreferSentenceBoundaryNearWindowEnd() {
        String text = "a b c d e f g h. i j k l m n";
        List<String> chunks = chunker.chunk(text, 9, 1);
        assertThat(chunks.get(0)).endsWith("h.");
    }

    @Test
    void shouldUseConfigDefaultsWhenOverridesNull() {
        properties.getTokenWindow().setChunkSize(4);
        properties.getTokenWindow().setOverlap(1);
        List<String> chunks = chunker.chunk("a b c d e f g", null, null);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).split("\\s+")).hasSize(4);
    }
}
