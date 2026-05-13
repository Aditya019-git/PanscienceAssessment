package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.entity.StoredFile;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class OpenAiAnswerGenerationServiceTest {

    @Mock private RestClient.Builder restClientBuilder;
    @Mock private WebClient.Builder webClientBuilder;
    @Mock private RestClient restClient;
    @Mock private WebClient webClient;

    private OpenAiAnswerGenerationService service;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.defaultHeader(anyString(), any(String[].class))).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);
        
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeader(anyString(), any(String[].class))).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        service = new OpenAiAnswerGenerationService(restClientBuilder, webClientBuilder, "sk-test", "gpt-4");
    }

    @Test
    void generatesAnswerSuccessfully() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        var message = new OpenAiAnswerGenerationService.ChatMessage("assistant", "The answer is here.");
        var choice = new OpenAiAnswerGenerationService.Choice(message);
        var response = new OpenAiAnswerGenerationService.OpenAiChatResponse(List.of(choice));
        when(responseSpec.body(OpenAiAnswerGenerationService.OpenAiChatResponse.class)).thenReturn(response);

        StoredFile file = new StoredFile();
        file.setOriginalName("doc.pdf");
        ContentChunk chunk = new ContentChunk();
        chunk.setChunkText("Source text");
        chunk.setPageNumber(5);

        String answer = service.generateAnswer(file, "What?", List.of(new RetrievedChunk(chunk, 0.5)));

        assertThat(answer).isEqualTo("The answer is here.");
    }

    @Test
    void streamAnswerProducesTokens() {
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), any(String[].class))).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        var chunk1 = new OpenAiAnswerGenerationService.OpenAiChatChunkResponse(List.of(
            new OpenAiAnswerGenerationService.ChunkChoice(new OpenAiAnswerGenerationService.ChunkDelta("Part1"))
        ));
        var chunk2 = new OpenAiAnswerGenerationService.OpenAiChatChunkResponse(List.of(
            new OpenAiAnswerGenerationService.ChunkChoice(new OpenAiAnswerGenerationService.ChunkDelta("Part2"))
        ));

        when(responseSpec.bodyToFlux(OpenAiAnswerGenerationService.OpenAiChatChunkResponse.class))
            .thenReturn(Flux.just(chunk1, chunk2));

        StoredFile file = new StoredFile();
        file.setOriginalName("video.mp4");
        ContentChunk chunk = new ContentChunk();
        chunk.setChunkText("Transcript line");
        chunk.setStartTime(10.0);
        chunk.setEndTime(15.5);

        Flux<String> result = service.streamAnswer(file, "When?", List.of(new RetrievedChunk(chunk, 0.8)));

        StepVerifier.create(result)
            .expectNext("Part1")
            .expectNext("Part2")
            .verifyComplete();
    }

    @Test
    void throwsOnBlankAnswer() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);

        var response = new OpenAiAnswerGenerationService.OpenAiChatResponse(List.of(
            new OpenAiAnswerGenerationService.Choice(new OpenAiAnswerGenerationService.ChatMessage("assistant", ""))
        ));
        when(responseSpec.body(OpenAiAnswerGenerationService.OpenAiChatResponse.class)).thenReturn(response);

        assertThatThrownBy(() -> service.generateAnswer(new StoredFile(), "Q", List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("blank answer");
    }

    @Test
    void isAvailableReturnsTrueWhenApiKeySet() {
        assertThat(service.isAvailable()).isTrue();
    }

    @Test
    void isAvailableReturnsFalseWhenApiKeyMissing() {
        OpenAiAnswerGenerationService noKeyService = new OpenAiAnswerGenerationService(
            restClientBuilder, webClientBuilder, "", "gpt-4"
        );
        assertThat(noKeyService.isAvailable()).isFalse();
    }

    @Test
    void generateAnswerThrowsWhenUnavailable() {
        OpenAiAnswerGenerationService noKeyService = new OpenAiAnswerGenerationService(
            restClientBuilder, webClientBuilder, "", "gpt-4"
        );
        assertThatThrownBy(() -> noKeyService.generateAnswer(new StoredFile(), "Q", List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPENAI_API_KEY is not configured");
    }

    @Test
    void generateAnswerThrowsOnRestClientException() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenThrow(new org.springframework.web.client.RestClientException("Network error"));

        assertThatThrownBy(() -> service.generateAnswer(new StoredFile(), "Q", List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("request failed");
    }

    @Test
    void generateAnswerThrowsOnEmptyResponse() {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAiAnswerGenerationService.OpenAiChatResponse.class)).thenReturn(null);

        assertThatThrownBy(() -> service.generateAnswer(new StoredFile(), "Q", List.of()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("empty response");
    }

    @Test
    void streamAnswerReturnsErrorWhenUnavailable() {
        OpenAiAnswerGenerationService noKeyService = new OpenAiAnswerGenerationService(
            restClientBuilder, webClientBuilder, "", "gpt-4"
        );
        Flux<String> result = noKeyService.streamAnswer(new StoredFile(), "Q", List.of());
        StepVerifier.create(result)
            .expectError(IllegalStateException.class)
            .verify();
    }

    @Test
    void formatTimeHandlesNull() {
        StoredFile file = new StoredFile();
        file.setOriginalName("test.mp3");
        ContentChunk chunk = new ContentChunk();
        chunk.setChunkText("text");
        chunk.setStartTime(null);
        chunk.setEndTime(null);

        // Access via generateAnswer buildPrompt
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        org.mockito.Mockito.lenient().doReturn(requestBodySpec).when(requestBodySpec).header(anyString(), anyString());
        org.mockito.Mockito.lenient().doReturn(requestBodySpec).when(requestBodySpec).body(org.mockito.ArgumentMatchers.any(Object.class)); 
        org.mockito.Mockito.lenient().doReturn(responseSpec).when(requestBodySpec).retrieve();
        
        var message = new OpenAiAnswerGenerationService.ChatMessage("assistant", "A");
        var choice = new OpenAiAnswerGenerationService.Choice(message);
        var response = new OpenAiAnswerGenerationService.OpenAiChatResponse(List.of(choice));
        when(responseSpec.body(OpenAiAnswerGenerationService.OpenAiChatResponse.class)).thenReturn(response);

        // We just need to trigger the buildPrompt/formatTime logic
        service.generateAnswer(file, "Q", List.of(new RetrievedChunk(chunk, 1.0)));
        // If it doesn't crash on null start/end time, it's covered.
    }
}
