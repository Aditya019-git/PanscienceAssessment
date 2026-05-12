package com.panscience.assessment.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class OpenAiEmbeddingService implements EmbeddingService {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingService(
        RestClient.Builder restClientBuilder,
        @Value("${app.openai.api-key:}") String apiKey,
        @Value("${app.openai.embedding-model:text-embedding-3-small}") String model
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
    public String model() {
        return model;
    }

    @Override
    public List<List<Double>> embed(List<String> inputs) {
        if (!isAvailable()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured for embeddings");
        }

        OpenAiEmbeddingsResponse response;
        try {
            response = restClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .body(new OpenAiEmbeddingsRequest(inputs, model, "float"))
                .retrieve()
                .body(OpenAiEmbeddingsResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("OpenAI embeddings request failed", exception);
        }

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("OpenAI embeddings returned an empty response");
        }

        return response.data().stream()
            .map(OpenAiEmbeddingData::embedding)
            .toList();
    }

    private record OpenAiEmbeddingsRequest(
        List<String> input,
        String model,
        String encoding_format
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiEmbeddingsResponse(List<OpenAiEmbeddingData> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiEmbeddingData(List<Double> embedding) {
    }
}
