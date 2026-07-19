package com.aisales.ai.application.rag;

/**
 * Pluggable document text extractor. Selected by {@code aisales.ai.rag.extractor}.
 */
public interface DocumentExtractor {

    /** Router key: {@code TEXT} | {@code PDF}. */
    String name();

    boolean supports(String contentType, String filename);

    String extract(byte[] content, String contentType, String filename);
}
