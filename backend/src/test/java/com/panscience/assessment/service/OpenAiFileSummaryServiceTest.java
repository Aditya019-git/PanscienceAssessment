package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.StoredFile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class OpenAiFileSummaryServiceTest {

    private RestClient.Builder builder;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    void setUp() {
        builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.defaultHeader(anyString(), any(String[].class))).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);
    }

    @Test
    void fallsBackWhenApiKeyIsMissing() {
        OpenAiFileSummaryService fileSummaryService = new OpenAiFileSummaryService(
            builder, "", "gpt-4.1-mini"
        );

        String summary = fileSummaryService.generateSummary(
            pdfFile(),
            List.of("Content about Spring Boot.")
        );

        assertThat(summary).startsWith("This PDF discusses:");
        assertThat(summary).contains("Spring Boot");
    }

    @Test
    void generatesSummarySuccessfully() {
        OpenAiFileSummaryService fileSummaryService = new OpenAiFileSummaryService(
            builder, "test-key", "gpt-4.1-mini"
        );

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        var message = new OpenAiFileSummaryService.ChatMessage("assistant", "This is a summary.");
        var choice = new OpenAiFileSummaryService.Choice(message);
        var response = new OpenAiFileSummaryService.OpenAiChatResponse(List.of(choice));
        when(responseSpec.body(OpenAiFileSummaryService.OpenAiChatResponse.class)).thenReturn(response);

        String summary = fileSummaryService.generateSummary(pdfFile(), List.of("Some text"));
        assertThat(summary).isEqualTo("This is a summary.");
    }

    private StoredFile pdfFile() {
        StoredFile storedFile = new StoredFile();
        ReflectionTestUtils.setField(storedFile, "id", 1L);
        storedFile.setOriginalName("spec.pdf");
        storedFile.setFileCategory(FileCategory.PDF);
        return storedFile;
    }
}
