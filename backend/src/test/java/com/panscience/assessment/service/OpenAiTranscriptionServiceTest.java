package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.panscience.assessment.exception.TranscriptionUnavailableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

class OpenAiTranscriptionServiceTest {

    private RestClient.Builder builder;
    private RestClient restClient;
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    private RestClient.RequestBodySpec requestBodySpec;
    private RestClient.ResponseSpec responseSpec;

    private OpenAiTranscriptionService transcriptionService;

    @BeforeEach
    void setUp() {
        builder = mock(RestClient.Builder.class);
        restClient = mock(RestClient.class);
        requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        requestBodySpec = mock(RestClient.RequestBodySpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(restClient);

        transcriptionService = new OpenAiTranscriptionService(builder, "test-key", "openai", "whisper-1");
    }

    @Test
    void rejectsTranscriptionWhenApiKeyIsMissing() throws Exception {
        Path mediaFile = Files.createTempFile("panscience-audio-", ".mp3");
        OpenAiTranscriptionService noKeyService = new OpenAiTranscriptionService(
            builder, "", "openai", "whisper-1"
        );

        assertThatThrownBy(() -> noKeyService.transcribe(mediaFile))
            .isInstanceOf(TranscriptionUnavailableException.class)
            .hasMessageContaining("OPENAI_API_KEY");

        Files.deleteIfExists(mediaFile);
    }

    @Test
    void transcribesSuccessfully() throws Exception {
        Path mediaFile = Files.createTempFile("test-audio", ".mp3");
        Files.writeString(mediaFile, "audio-content");

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(org.springframework.http.MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(org.springframework.util.MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        OpenAiTranscriptionService.OpenAiSegment segment = new OpenAiTranscriptionService.OpenAiSegment(1, 0.0, 1.0, "Hello");
        OpenAiTranscriptionService.OpenAiTranscriptionResponse response = new OpenAiTranscriptionService.OpenAiTranscriptionResponse("Hello", List.of(segment));
        when(responseSpec.body(OpenAiTranscriptionService.OpenAiTranscriptionResponse.class)).thenReturn(response);

        TranscriptionResult result = transcriptionService.transcribe(mediaFile);

        assertThat(result.transcriptText()).isEqualTo("Hello");
        assertThat(result.segments()).hasSize(1);

        Files.deleteIfExists(mediaFile);
    }

    @Test
    void rejectsWhenProviderIsNotOpenAi() throws Exception {
        Path mediaFile = Files.createTempFile("panscience-audio-", ".mp3");
        OpenAiTranscriptionService otherProvider = new OpenAiTranscriptionService(
            builder, "key", "other", "model"
        );
        assertThatThrownBy(() -> otherProvider.transcribe(mediaFile))
            .isInstanceOf(TranscriptionUnavailableException.class)
            .hasMessageContaining("Only the OpenAI transcription provider");
        Files.deleteIfExists(mediaFile);
    }

    @Test
    void rejectsWhenFileIsTooLarge() throws Exception {
        Path mediaFile = Files.createTempFile("large-audio-", ".mp3");
        // 26 MB > 25 MB
        byte[] largeData = new byte[26 * 1024 * 1024];
        Files.write(mediaFile, largeData);
        
        assertThatThrownBy(() -> transcriptionService.transcribe(mediaFile))
            .isInstanceOf(TranscriptionUnavailableException.class)
            .hasMessageContaining("exceeds the 25 MB");
        Files.deleteIfExists(mediaFile);
    }

    @Test
    void handlesEmptyResponse() throws Exception {
        Path mediaFile = Files.createTempFile("audio-", ".mp3");
        Files.writeString(mediaFile, "content");
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(org.springframework.http.MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(org.springframework.util.MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAiTranscriptionService.OpenAiTranscriptionResponse.class)).thenReturn(null);

        assertThatThrownBy(() -> transcriptionService.transcribe(mediaFile))
            .isInstanceOf(TranscriptionUnavailableException.class)
            .hasMessageContaining("empty response");
        Files.deleteIfExists(mediaFile);
    }

    @Test
    void handlesNullSegmentsInResponse() throws Exception {
        Path mediaFile = Files.createTempFile("audio-", ".mp3");
        Files.writeString(mediaFile, "content");
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(org.springframework.http.MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(org.springframework.util.MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        
        OpenAiTranscriptionService.OpenAiTranscriptionResponse response = new OpenAiTranscriptionService.OpenAiTranscriptionResponse("No segments", null);
        when(responseSpec.body(OpenAiTranscriptionService.OpenAiTranscriptionResponse.class)).thenReturn(response);

        TranscriptionResult result = transcriptionService.transcribe(mediaFile);
        assertThat(result.transcriptText()).isEqualTo("No segments");
        assertThat(result.segments()).isEmpty();
        
        Files.deleteIfExists(mediaFile);
    }

    @Test
    void handlesRestClientException() throws Exception {
        Path mediaFile = Files.createTempFile("audio-", ".mp3");
        Files.writeString(mediaFile, "content");
        
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(org.springframework.http.MediaType.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(org.springframework.util.MultiValueMap.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(OpenAiTranscriptionService.OpenAiTranscriptionResponse.class))
            .thenThrow(new org.springframework.web.client.RestClientException("Network error"));

        assertThatThrownBy(() -> transcriptionService.transcribe(mediaFile))
            .isInstanceOf(TranscriptionUnavailableException.class)
            .hasMessageContaining("request failed");
        Files.deleteIfExists(mediaFile);
    }
}
