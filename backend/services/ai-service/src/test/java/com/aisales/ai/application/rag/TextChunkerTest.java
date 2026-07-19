package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import com.aisales.common.exception.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TextChunkerTest {

    private RagProperties properties;
    private TextChunker chunker;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        chunker = new TextChunker(
                List.of(new CharWindowChunker(properties), new TokenWindowChunker(properties)),
                properties);
    }

    @Test
    void shouldRouteToTokenByDefault() {
        properties.setChunker("TOKEN");
        assertThat(chunker.name()).isEqualTo("TOKEN");
        List<String> chunks = chunker.chunk("one two three four five", 3, 1);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0).split("\\s+")).hasSize(3);
    }

    @Test
    void shouldSwitchToCharWhenConfigured() {
        properties.setChunker("CHAR");
        assertThat(chunker.name()).isEqualTo("CHAR");
        String text = "a".repeat(50);
        List<String> chunks = chunker.chunk(text, 20, 5);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks.get(0)).hasSize(20);
    }

    @Test
    void shouldReturnEmptyForBlank() {
        assertThat(chunker.chunk("   ", null, null)).isEmpty();
        assertThat(chunker.chunk(null, null, null)).isEmpty();
    }

    @Test
    void shouldFailForUnknownChunker() {
        properties.setChunker("UNKNOWN");
        assertThatThrownBy(() -> chunker.chunk("text", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("UNKNOWN");
    }
}
