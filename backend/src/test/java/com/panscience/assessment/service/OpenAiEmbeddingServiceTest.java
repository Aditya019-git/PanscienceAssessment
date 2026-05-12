package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiEmbeddingServiceTest {

    private RestClient.Builder builder;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    private OpenAiEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        embeddingService = new OpenAiEmbeddingService(builder, "test-key", "test-model");
    }

    @Test
    void embedsSuccessfully() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        var data = new OpenAiEmbeddingService.OpenAiEmbeddingData(List.of(0.1, 0.2));
        var response = new OpenAiEmbeddingService.OpenAiEmbeddingsResponse(List.of(data));
        when(responseSpec.body(OpenAiEmbeddingService.OpenAiEmbeddingsResponse.class)).thenReturn(response);

        List<List<Double>> result = embeddingService.embed(List.of("hello"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsExactly(0.1, 0.2);
    }

    @Test
    void isAvailableWhenApiKeyPresent() {
        assertThat(embeddingService.isAvailable()).isTrue();
    }

    @Test
    void isNotAvailableWhenApiKeyMissing() {
        OpenAiEmbeddingService unavailableService = new OpenAiEmbeddingService(builder, "", "model");
        assertThat(unavailableService.isAvailable()).isFalse();
    }

    @Test
    void throwsWhenEmbeddingAndNotAvailable() {
        OpenAiEmbeddingService unavailableService = new OpenAiEmbeddingService(builder, "", "model");
        assertThatThrownBy(() -> unavailableService.embed(List.of("test")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPENAI_API_KEY is not configured");
    }
}
