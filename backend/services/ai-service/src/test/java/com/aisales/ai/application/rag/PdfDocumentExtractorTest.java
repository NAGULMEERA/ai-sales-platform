package com.aisales.ai.application.rag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfDocumentExtractorTest {

    private final PdfDocumentExtractor extractor = new PdfDocumentExtractor();

    @Test
    void shouldSupportPdfContentType() {
        assertThat(extractor.supports("application/pdf", "doc.pdf")).isTrue();
        assertThat(extractor.supports("text/plain", "notes.txt")).isFalse();
    }

    @Test
    void shouldExtractTextFromPdf() throws IOException {
        byte[] pdf = minimalPdf("Warranty covers three years.");
        String text = extractor.extract(pdf, "application/pdf", "warranty.pdf");
        assertThat(text).contains("Warranty");
    }

    private static byte[] minimalPdf(String line) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(line);
                content.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
