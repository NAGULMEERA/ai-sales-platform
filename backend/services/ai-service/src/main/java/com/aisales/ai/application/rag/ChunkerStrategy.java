package com.aisales.ai.application.rag;

import java.util.List;

/**
 * Pluggable text chunking strategy. Selected by {@code aisales.ai.rag.chunker}.
 */
public interface ChunkerStrategy {

    /** Router key: {@code CHAR} or {@code TOKEN}. */
    String name();

    List<String> chunk(String text, Integer sizeOverride, Integer overlapOverride);
}
