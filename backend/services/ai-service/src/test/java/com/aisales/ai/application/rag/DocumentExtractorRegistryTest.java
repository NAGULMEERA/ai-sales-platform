package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.ai.infrastructure.configuration.RagProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentExtractorRegistryTest {

    @Test
    void shouldAutoSelectPdf() {
        RagProperties properties = new RagProperties();
        properties.setExtractor("AUTO");
        DocumentExtractorRegistry registry = new DocumentExtractorRegistry(
                List.of(new TextDocumentExtractor(), new PdfDocumentExtractor()), properties);

        assertThat(registry.resolve(null, "application/pdf", "a.pdf").name()).isEqualTo("PDF");
        assertThat(registry.resolve(null, "text/plain", "a.txt").name()).isEqualTo("TEXT");
        assertThat(registry.resolve("PDF", "text/plain", "a.txt").name()).isEqualTo("PDF");
    }
}
