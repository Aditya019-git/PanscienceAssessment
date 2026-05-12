package com.panscience.assessment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.panscience.assessment.entity.StoredFile;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiAnswerGenerationService implements AnswerGenerationService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiAnswerGenerationService(
        RestClient.Builder restClientBuilder,
        @Value("${app.openai.api-key:}") String apiKey,
        @Value("${app.openai.model:gpt-4.1-mini}") String model
    ) {
        this.restClient = restClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public boolean isAvailable() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public String generateAnswer(StoredFile storedFile, String question, List<RetrievedChunk> retrievedChunks) {
        if (!isAvailable()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured for answer generation");
        }

        List<ChatMessage> messages = List.of(
            new ChatMessage(
                "system",
                """
                You answer questions about a single uploaded document or media transcript.
                Use only the provided context.
                Cite supporting context inline using bracketed source numbers like [1] or [2].
                If the answer is not supported by the context, say that you could not find it in the uploaded file.
                """
            ),
            new ChatMessage("user", buildPrompt(storedFile, question, retrievedChunks))
        );

        OpenAiChatResponse response;
        try {
            response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(new ChatCompletionRequest(model, messages, 0.2))
                .retrieve()
                .body(OpenAiChatResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("OpenAI answer generation request failed", exception);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI answer generation returned an empty response");
        }

        String content = response.choices().get(0).message() == null
            ? null
            : response.choices().get(0).message().content();

        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("OpenAI answer generation returned a blank answer");
        }

        return content.trim();
    }

    private String buildPrompt(StoredFile storedFile, String question, List<RetrievedChunk> retrievedChunks) {
        StringBuilder builder = new StringBuilder();
        builder.append("File name: ").append(storedFile.getOriginalName()).append("\n");
        builder.append("Question: ").append(question).append("\n\n");
        builder.append("Context:\n");

        for (int index = 0; index < retrievedChunks.size(); index++) {
            RetrievedChunk retrievedChunk = retrievedChunks.get(index);
            builder.append('[').append(index + 1).append("] ");

            if (retrievedChunk.chunk().getPageNumber() != null) {
                builder.append("Page ").append(retrievedChunk.chunk().getPageNumber()).append(" ");
            }

            if (retrievedChunk.chunk().getStartTime() != null || retrievedChunk.chunk().getEndTime() != null) {
                builder.append("Timestamp ");
                builder.append(formatTime(retrievedChunk.chunk().getStartTime())).append(" to ");
                builder.append(formatTime(retrievedChunk.chunk().getEndTime())).append(" ");
            }

            builder.append("- ").append(retrievedChunk.chunk().getChunkText().replaceAll("\\s+", " ").trim()).append("\n\n");
        }

        builder.append("Answer using only the context above.");
        return builder.toString();
    }

    private String formatTime(Double seconds) {
        if (seconds == null) {
            return "0.0s";
        }

        return String.format("%.1fs", seconds);
    }

    private record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        double temperature
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(ChatMessage message) {
    }
}
