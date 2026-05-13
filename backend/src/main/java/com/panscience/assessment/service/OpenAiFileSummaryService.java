package com.panscience.assessment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.StoredFile;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiFileSummaryService implements FileSummaryService {

    private static final int MAX_FALLBACK_PARTS = 3;
    private static final int MAX_PROMPT_LENGTH = 4000;

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiFileSummaryService(
        RestClient.Builder restClientBuilder,
        @Value("${app.openai.api-key:}") String apiKey,
        @Value("${app.openai.model:gpt-4o-mini}") String model
    ) {
        this.restClient = restClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generateSummary(StoredFile storedFile, List<String> contentParts) {
        List<String> normalizedParts = contentParts.stream()
            .filter(StringUtils::hasText)
            .map(part -> part.replaceAll("\\s+", " ").trim())
            .filter(StringUtils::hasText)
            .toList();

        if (normalizedParts.isEmpty()) {
            return "No extractable content was found in the uploaded file.";
        }

        if (!StringUtils.hasText(apiKey)) {
            return fallbackSummary(storedFile, normalizedParts);
        }

        try {
            return modelSummary(storedFile, normalizedParts);
        } catch (RuntimeException exception) {
            return fallbackSummary(storedFile, normalizedParts);
        }
    }

    private String modelSummary(StoredFile storedFile, List<String> contentParts) {
        String prompt = buildPrompt(storedFile, contentParts);
        OpenAiChatResponse response;

        try {
            response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(new ChatCompletionRequest(
                    model,
                    List.of(
                        new ChatMessage(
                            "system",
                            "Write a concise 2-3 sentence summary of the uploaded file using only the provided content."
                        ),
                        new ChatMessage("user", prompt)
                    ),
                    0.2
                ))
                .retrieve()
                .body(OpenAiChatResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("OpenAI summary generation request failed", exception);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI summary generation returned an empty response");
        }

        String content = response.choices().get(0).message() == null
            ? null
            : response.choices().get(0).message().content();

        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("OpenAI summary generation returned a blank summary");
        }

        return content.trim();
    }

    private String buildPrompt(StoredFile storedFile, List<String> contentParts) {
        String joined = contentParts.stream()
            .limit(8)
            .collect(Collectors.joining("\n\n"));

        if (joined.length() > MAX_PROMPT_LENGTH) {
            joined = joined.substring(0, MAX_PROMPT_LENGTH);
        }

        return "File name: " + storedFile.getOriginalName()
            + "\nFile category: " + storedFile.getFileCategory()
            + "\n\nContent:\n"
            + joined;
    }

    private String fallbackSummary(StoredFile storedFile, List<String> contentParts) {
        String joined = contentParts.stream()
            .limit(MAX_FALLBACK_PARTS)
            .collect(Collectors.joining(" "));

        String normalized = joined.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 260) {
            normalized = normalized.substring(0, 257) + "...";
        }

        if (storedFile.getFileCategory() == FileCategory.PDF) {
            return "This PDF discusses: " + normalized;
        }

        return "This media file covers: " + normalized;
    }

    record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        double temperature
    ) {
    }

    record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OpenAiChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {
    }
}
