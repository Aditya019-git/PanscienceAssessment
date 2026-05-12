package com.panscience.assessment.service;

import com.panscience.assessment.exception.FileProcessingException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PdfExtractionService {

    public List<ExtractedPdfChunk> extract(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            List<ExtractedPdfChunk> chunks = new ArrayList<>();

            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String extractedText = stripper.getText(document).trim();

                if (StringUtils.hasText(extractedText)) {
                    chunks.add(new ExtractedPdfChunk(page, extractedText));
                }
            }

            return chunks;
        } catch (IOException exception) {
            throw new FileProcessingException("Failed to extract text from PDF " + pdfPath.getFileName(), exception);
        }
    }
}

