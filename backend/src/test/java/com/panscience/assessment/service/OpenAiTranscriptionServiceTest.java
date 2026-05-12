package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.panscience.assessment.exception.TranscriptionUnavailableException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAiTranscriptionServiceTest {

    @Test
    void rejectsTranscriptionWhenApiKeyIsMissing() throws Exception {
        Path mediaFile = Files.createTempFile("panscience-audio-", ".mp3");
        OpenAiTranscriptionService transcriptionService = new OpenAiTranscriptionService(
            RestClient.builder(),
            "",
            "openai",
            "whisper-1"
        );

        assertThatThrownBy(() -> transcriptionService.transcribe(mediaFile))
            .isInstanceOf(TranscriptionUnavailableException.class)
            .hasMessageContaining("OPENAI_API_KEY");

        Files.deleteIfExists(mediaFile);
    }
}
