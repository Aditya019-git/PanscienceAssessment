package com.panscience.assessment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.panscience.assessment.exception.TranscriptionUnavailableException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiTranscriptionService implements TranscriptionService {

    private static final long MAX_AUDIO_UPLOAD_BYTES = 25L * 1024L * 1024L;

    private final RestClient restClient;
    private final String apiKey;
    private final String provider;
    private final String model;

    public OpenAiTranscriptionService(
        RestClient.Builder restClientBuilder,
        @Value("${app.openai.api-key:}") String apiKey,
        @Value("${app.transcription.provider:openai}") String provider,
        @Value("${app.transcription.model:whisper-1}") String model
    ) {
        this.restClient = restClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .build();
        this.apiKey = apiKey;
        this.provider = provider;
        this.model = model;
    }

    @Override
    public TranscriptionResult transcribe(Path mediaFilePath) {
        if (!"openai".equalsIgnoreCase(provider)) {
            throw new TranscriptionUnavailableException("Only the OpenAI transcription provider is currently implemented");
        }

        if (!StringUtils.hasText(apiKey)) {
            throw new TranscriptionUnavailableException("OPENAI_API_KEY is not configured for audio or video transcription");
        }

        validateFileSize(mediaFilePath);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(mediaFilePath));
        body.add("model", model);
        body.add("response_format", "verbose_json");
        body.add("timestamp_granularities[]", "segment");

        try {
            OpenAiTranscriptionResponse response = restClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(OpenAiTranscriptionResponse.class);

            if (response == null) {
                throw new TranscriptionUnavailableException("OpenAI transcription returned an empty response");
            }

            List<TranscribedSegment> segments = response.segments() == null
                ? List.of()
                : response.segments().stream()
                    .map(segment -> new TranscribedSegment(
                        segment.text(),
                        segment.start(),
                        segment.end(),
                        segment.id()
                    ))
                    .toList();

            return new TranscriptionResult(response.text(), segments);
        } catch (RestClientException exception) {
            throw new TranscriptionUnavailableException("OpenAI transcription request failed", exception);
        }
    }

    private void validateFileSize(Path mediaFilePath) {
        try {
            long size = Files.size(mediaFilePath);
            if (size > MAX_AUDIO_UPLOAD_BYTES) {
                throw new TranscriptionUnavailableException("Media file exceeds the 25 MB OpenAI transcription limit");
            }
        } catch (IOException exception) {
            throw new TranscriptionUnavailableException("Could not determine media file size before transcription", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiTranscriptionResponse(
        String text,
        List<OpenAiSegment> segments
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiSegment(
        int id,
        double start,
        double end,
        String text
    ) {
    }
}

