package com.aisales.ai.application.rag;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Plain-text / markdown extractor. Selected when {@code aisales.ai.rag.extractor=TEXT}
 * or AUTO resolves to text.
 */
@Component
public class TextDocumentExtractor implements DocumentExtractor {

    public static final String NAME = "TEXT";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String contentType, String filename) {
        String type = contentType != null ? contentType.toLowerCase() : "";
        String name = filename != null ? filename.toLowerCase() : "";
        return type.startsWith("text/")
                || type.contains("json")
                || name.endsWith(".txt")
                || name.endsWith(".md")
                || name.endsWith(".csv");
    }

    @Override
    public String extract(byte[] content, String contentType, String filename) {
        if (content == null || content.length == 0) {
            return "";
        }
        Charset charset = StandardCharsets.UTF_8;
        return new String(content, charset).trim();
    }
}
