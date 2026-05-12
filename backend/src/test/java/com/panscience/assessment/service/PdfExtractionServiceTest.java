package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import com.panscience.assessment.exception.FileProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;

class PdfExtractionServiceTest {

    private final PdfExtractionService pdfExtractionService = new PdfExtractionService();

    @Test
    void extractsTextPerPage() throws IOException {
        Path pdfPath = Files.createTempFile("panscience-", ".pdf");

        try (PDDocument document = new PDDocument()) {
            document.addPage(pageWithText(document, "Page one"));
            document.addPage(pageWithText(document, "Page two"));
            document.save(pdfPath.toFile());
        }

        List<ExtractedPdfChunk> extractedChunks = pdfExtractionService.extract(pdfPath);

        assertThat(extractedChunks).hasSize(2);
        assertThat(extractedChunks.get(0).pageNumber()).isEqualTo(1);
        assertThat(extractedChunks.get(0).text()).contains("Page one");
        assertThat(extractedChunks.get(1).pageNumber()).isEqualTo(2);
        assertThat(extractedChunks.get(1).text()).contains("Page two");

        Files.deleteIfExists(pdfPath);
    }

    @Test
    void throwsFileProcessingExceptionOnInvalidPdf() {
        Path invalidPath = Path.of("non-existent.pdf");
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> pdfExtractionService.extract(invalidPath))
            .isInstanceOf(FileProcessingException.class)
            .hasMessageContaining("Failed to extract text");
    }

    private PDPage pageWithText(PDDocument document, String text) throws IOException {
        PDPage page = new PDPage();

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText(text);
            contentStream.endText();
        }

        return page;
    }
}

