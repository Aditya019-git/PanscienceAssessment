package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.StoredFile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class OpenAiFileSummaryServiceTest {

    @Test
    void fallsBackWhenApiKeyIsMissing() {
        OpenAiFileSummaryService fileSummaryService = new OpenAiFileSummaryService(
            RestClient.builder(),
            "",
            "gpt-4.1-mini"
        );

        String summary = fileSummaryService.generateSummary(
            pdfFile(),
            List.of(
                "Spring Boot handles uploads and processing.",
                "OpenAI is used for retrieval and question answering."
            )
        );

        assertThat(summary).startsWith("This PDF discusses:");
        assertThat(summary).contains("Spring Boot");
    }

    @Test
    void returnsEmptyContentFallbackWhenNoContentExists() {
        OpenAiFileSummaryService fileSummaryService = new OpenAiFileSummaryService(
            RestClient.builder(),
            "",
            "gpt-4.1-mini"
        );

        String summary = fileSummaryService.generateSummary(pdfFile(), List.of(" ", ""));

        assertThat(summary).isEqualTo("No extractable content was found in the uploaded file.");
    }

    private StoredFile pdfFile() {
        StoredFile storedFile = new StoredFile();
        ReflectionTestUtils.setField(storedFile, "id", 1L);
        storedFile.setOriginalName("spec.pdf");
        storedFile.setFileCategory(FileCategory.PDF);
        storedFile.setStoredName("stored-spec.pdf");
        storedFile.setContentType("application/pdf");
        storedFile.setStoragePath("uploads/spec.pdf");
        return storedFile;
    }
}
