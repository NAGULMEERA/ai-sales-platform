package com.aisales.ai.application.rag;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * PDF text extractor (Apache PDFBox). Selected when {@code aisales.ai.rag.extractor=PDF}
 * or AUTO resolves to PDF.
 */
@Component
public class PdfDocumentExtractor implements DocumentExtractor {

    public static final String NAME = "PDF";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean supports(String contentType, String filename) {
        String type = contentType != null ? contentType.toLowerCase() : "";
        String name = filename != null ? filename.toLowerCase() : "";
        return type.contains("pdf") || name.endsWith(".pdf");
    }

    @Override
    public String extract(byte[] content, String contentType, String filename) {
        if (content == null || content.length == 0) {
            return "";
        }
        try (PDDocument document = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return StringUtils.hasText(text) ? text.trim() : "";
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to extract text from PDF", ex);
        }
    }
}
