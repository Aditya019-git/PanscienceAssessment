package com.panscience.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panscience.assessment.entity.ChunkEmbedding;
import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.repository.ChunkEmbeddingRepository;
import com.panscience.assessment.repository.ContentChunkRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ContentRetrievalService {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final TypeReference<List<Double>> DOUBLE_LIST_TYPE = new TypeReference<>() {};

    private final ContentChunkRepository contentChunkRepository;
    private final ChunkEmbeddingRepository chunkEmbeddingRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    public ContentRetrievalService(
        ContentChunkRepository contentChunkRepository,
        ChunkEmbeddingRepository chunkEmbeddingRepository,
        EmbeddingService embeddingService,
        ObjectMapper objectMapper
    ) {
        this.contentChunkRepository = contentChunkRepository;
        this.chunkEmbeddingRepository = chunkEmbeddingRepository;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<RetrievedChunk> retrieveRelevantChunks(Long fileId, String question, int limit) {
        List<ContentChunk> chunks = contentChunkRepository.findAllByFileIdOrderByIdAsc(fileId);
        if (chunks.isEmpty() || !StringUtils.hasText(question)) {
            return List.of();
        }

        Map<Long, Double> lexicalScores = scoreLexically(chunks, question);
        List<RetrievedChunk> lexicalResults = lexicalResults(chunks, lexicalScores, limit);

        if (!embeddingService.isAvailable()) {
            return lexicalResults;
        }

        try {
            List<Double> questionEmbedding = embeddingService.embed(List.of(question)).get(0);
            Map<Long, List<Double>> storedEmbeddings = ensureEmbeddings(chunks);

            return chunks.stream()
                .map(chunk -> new RetrievedChunk(
                    chunk,
                    hybridScore(
                        questionEmbedding,
                        storedEmbeddings.get(chunk.getId()),
                        lexicalScores.getOrDefault(chunk.getId(), 0.0)
                    )
                ))
                .filter(retrievedChunk -> retrievedChunk.score() > 0.0)
                .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
                .limit(limit)
                .toList();
        } catch (RuntimeException exception) {
            return lexicalResults;
        }
    }

    private Map<Long, List<Double>> ensureEmbeddings(List<ContentChunk> chunks) {
        List<Long> chunkIds = chunks.stream().map(ContentChunk::getId).toList();
        String embeddingModel = embeddingService.model();

        Map<Long, List<Double>> storedEmbeddings = new LinkedHashMap<>();
        chunkEmbeddingRepository.findAllByChunkIdInAndEmbeddingModel(chunkIds, embeddingModel)
            .forEach(embedding -> storedEmbeddings.put(
                embedding.getChunkId(),
                deserializeEmbedding(embedding.getEmbeddingVector())
            ));

        List<ContentChunk> missingChunks = chunks.stream()
            .filter(chunk -> !storedEmbeddings.containsKey(chunk.getId()))
            .filter(chunk -> StringUtils.hasText(chunk.getChunkText()))
            .toList();

        if (missingChunks.isEmpty()) {
            return storedEmbeddings;
        }

        List<List<Double>> generatedEmbeddings = embeddingService.embed(
            missingChunks.stream().map(ContentChunk::getChunkText).toList()
        );

        List<ChunkEmbedding> newEmbeddings = new ArrayList<>();
        for (int index = 0; index < missingChunks.size(); index++) {
            ContentChunk chunk = missingChunks.get(index);
            List<Double> vector = generatedEmbeddings.get(index);
            ChunkEmbedding chunkEmbedding = new ChunkEmbedding();
            chunkEmbedding.setChunkId(chunk.getId());
            chunkEmbedding.setEmbeddingModel(embeddingModel);
            chunkEmbedding.setEmbeddingVector(serializeEmbedding(vector));
            newEmbeddings.add(chunkEmbedding);
            storedEmbeddings.put(chunk.getId(), vector);
            chunk.setEmbeddingRef(embeddingModel);
        }

        chunkEmbeddingRepository.saveAll(newEmbeddings);
        contentChunkRepository.saveAll(missingChunks);

        return storedEmbeddings;
    }

    private List<RetrievedChunk> lexicalResults(List<ContentChunk> chunks, Map<Long, Double> lexicalScores, int limit) {
        return chunks.stream()
            .map(chunk -> new RetrievedChunk(chunk, lexicalScores.getOrDefault(chunk.getId(), 0.0)))
            .filter(retrievedChunk -> retrievedChunk.score() > 0.0)
            .sorted(Comparator.comparingDouble(RetrievedChunk::score).reversed())
            .limit(limit)
            .toList();
    }

    private Map<Long, Double> scoreLexically(Collection<ContentChunk> chunks, String question) {
        Set<String> questionTokens = tokenize(question);
        Map<Long, Double> scores = new HashMap<>();

        for (ContentChunk chunk : chunks) {
            Set<String> chunkTokens = tokenize(chunk.getChunkText());
            if (questionTokens.isEmpty() || chunkTokens.isEmpty()) {
                scores.put(chunk.getId(), 0.0);
                continue;
            }

            long overlap = questionTokens.stream().filter(chunkTokens::contains).count();
            scores.put(chunk.getId(), overlap / (double) questionTokens.size());
        }

        return scores;
    }

    private Set<String> tokenize(String value) {
        if (!StringUtils.hasText(value)) {
            return Set.of();
        }

        String[] rawTokens = TOKEN_SPLIT_PATTERN.split(value.toLowerCase(Locale.ROOT));
        Set<String> tokens = new HashSet<>();
        for (String rawToken : rawTokens) {
            if (rawToken.length() >= 3) {
                tokens.add(rawToken);
            }
        }

        return tokens;
    }

    private double hybridScore(List<Double> queryEmbedding, List<Double> chunkEmbedding, double lexicalScore) {
        double embeddingScore = cosineSimilarity(queryEmbedding, chunkEmbedding);
        if (embeddingScore <= 0.0) {
            return lexicalScore;
        }

        return (embeddingScore * 0.85) + (lexicalScore * 0.15);
    }

    private double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.size() != right.size() || left.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double leftMagnitude = 0.0;
        double rightMagnitude = 0.0;

        for (int index = 0; index < left.size(); index++) {
            double leftValue = left.get(index);
            double rightValue = right.get(index);
            dotProduct += leftValue * rightValue;
            leftMagnitude += leftValue * leftValue;
            rightMagnitude += rightValue * rightValue;
        }

        if (leftMagnitude == 0.0 || rightMagnitude == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(leftMagnitude) * Math.sqrt(rightMagnitude));
    }

    private String serializeEmbedding(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize chunk embedding", exception);
        }
    }

    private List<Double> deserializeEmbedding(String serializedVector) {
        try {
            return objectMapper.readValue(serializedVector, DOUBLE_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize chunk embedding", exception);
        }
    }
}
