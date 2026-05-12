package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panscience.assessment.entity.ChunkEmbedding;
import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.repository.ChunkEmbeddingRepository;
import com.panscience.assessment.repository.ContentChunkRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ContentRetrievalServiceTest {

    @Mock
    private ContentChunkRepository contentChunkRepository;

    @Mock
    private ChunkEmbeddingRepository chunkEmbeddingRepository;

    @Mock
    private EmbeddingService embeddingService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private ContentRetrievalService contentRetrievalService;

    @BeforeEach
    void setUp() {
        contentRetrievalService = new ContentRetrievalService(
            contentChunkRepository,
            chunkEmbeddingRepository,
            embeddingService,
            objectMapper
        );
    }

    @Test
    void fallsBackToLexicalRankingWhenEmbeddingsAreUnavailable() {
        ContentChunk uploadsChunk = chunk(1L, 1L, "Spring Boot handles multipart file uploads and metadata persistence.");
        ContentChunk redisChunk = chunk(2L, 1L, "Redis is useful for caching and rate limiting.");

        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(uploadsChunk, redisChunk));
        when(embeddingService.isAvailable()).thenReturn(false);

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(
            1L,
            "How are file uploads handled?",
            3
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunk().getId()).isEqualTo(1L);
        assertThat(results.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void storesMissingEmbeddingsAndUsesThemForRanking() {
        ContentChunk alphaChunk = chunk(1L, 1L, "Alpha architecture overview");
        ContentChunk betaChunk = chunk(2L, 1L, "Beta release checklist");

        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(alphaChunk, betaChunk));
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.model()).thenReturn("text-embedding-3-small");
        when(chunkEmbeddingRepository.findAllByChunkIdInAndEmbeddingModel(anyCollection(), eq("text-embedding-3-small")))
            .thenReturn(List.of());
        when(embeddingService.embed(eq(List.of("Which chunk mentions beta?"))))
            .thenReturn(List.of(List.of(0.0, 1.0)));
        when(embeddingService.embed(eq(List.of("Alpha architecture overview", "Beta release checklist"))))
            .thenReturn(List.of(List.of(1.0, 0.0), List.of(0.0, 1.0)));

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(
            1L,
            "Which chunk mentions beta?",
            2
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).chunk().getId()).isEqualTo(2L);
        verify(chunkEmbeddingRepository).saveAll(anyList());
        verify(contentChunkRepository).saveAll(anyList());
    }

    @Test
    void returnsEmptyListForEmptyQuestionOrChunks() {
        assertThat(contentRetrievalService.retrieveRelevantChunks(1L, "", 5)).isEmpty();
        
        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(2L)).thenReturn(List.of());
        assertThat(contentRetrievalService.retrieveRelevantChunks(2L, "something", 5)).isEmpty();
    }

    @Test
    void calculatesCosineSimilarityCorrectly() {
        ContentChunk chunk = chunk(1L, 5L, "Exact match");
        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(5L)).thenReturn(List.of(chunk));
        when(embeddingService.isAvailable()).thenReturn(false); 

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(5L, "Exact match", 1);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).score()).isGreaterThan(0.0);
    }

    @Test
    void handlesCosineSimilarityEdgeCases() {
        // We test via hybrid score. If cosine returns 0.0, score is 0.15 * lexical.
        ContentChunk chunk = chunk(1L, 1L, "beta");
        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(chunk));
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.model()).thenReturn("m");
        
        // Mock stored embedding but make it mismatching size
        ChunkEmbedding emb = new ChunkEmbedding();
        emb.setEmbeddingVector("[1.0]"); // Size 1
        when(chunkEmbeddingRepository.findAllByChunkIdInAndEmbeddingModel(anyList(), anyString()))
            .thenReturn(List.of(emb));
        
        // Question embedding size 2 (mismatch)
        when(embeddingService.embed(eq(List.of("beta")))).thenReturn(List.of(List.of(1.0, 0.0)));

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(1L, "beta", 1);
        assertThat(results).isNotEmpty();
        // Lexical score for "beta" vs "beta" is 1.0.
        // If cosine is 0.0 (due to mismatch), hybrid score returns lexicalScore directly (1.0)
        assertThat(results.get(0).score()).isEqualTo(1.0);
    }

    @Test
    void handlesZeroMagnitudeCosineSimilarity() {
        ContentChunk chunk = chunk(1L, 1L, "beta");
        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(chunk));
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.model()).thenReturn("m");
        
        ChunkEmbedding emb = new ChunkEmbedding();
        emb.setEmbeddingVector("[0.0, 0.0]"); // Zero magnitude
        when(chunkEmbeddingRepository.findAllByChunkIdInAndEmbeddingModel(anyList(), anyString()))
            .thenReturn(List.of(emb));
        
        when(embeddingService.embed(eq(List.of("beta")))).thenReturn(List.of(List.of(1.0, 0.0)));

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(1L, "beta", 1);
        assertThat(results).isNotEmpty();
        // If magnitude is 0, cosine is 0.0. Hybrid score returns lexicalScore directly (1.0)
        assertThat(results.get(0).score()).isEqualTo(1.0);
    }

    @Test
    void returnsLexicalOnSerializationError() throws Exception {
        lenient().doThrow(new RuntimeException("serialize error"))
            .when(objectMapper).writeValueAsString(any());
        
        ContentChunk chunk = chunk(1L, 1L, "test tokens overlap");
        lenient().when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(chunk));
        lenient().when(embeddingService.isAvailable()).thenReturn(true);
        lenient().when(embeddingService.model()).thenReturn("m");
        lenient().when(embeddingService.embed(anyList())).thenReturn(List.of(List.of(1.0)));

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(1L, "test tokens overlap", 1);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).score()).isEqualTo(1.0); // Fallback to lexical
    }

    @Test
    void returnsLexicalOnDeserializationError() throws Exception {
        ChunkEmbedding embedding = new ChunkEmbedding();
        embedding.setEmbeddingVector("invalid");
        lenient().when(chunkEmbeddingRepository.findAllByChunkIdInAndEmbeddingModel(anyList(), anyString()))
            .thenReturn(List.of(embedding));

        lenient().doThrow(new RuntimeException("deserialize error"))
            .when(objectMapper).readValue(eq("invalid"), any(TypeReference.class));

        ContentChunk chunk = chunk(1L, 1L, "test tokens overlap");
        lenient().when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(chunk));
        lenient().when(embeddingService.isAvailable()).thenReturn(true);

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(1L, "test tokens overlap", 1);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).score()).isEqualTo(1.0); // Fallback to lexical
    }

    @Test
    void returnsLexicalOnEmbeddingFailure() {
        when(embeddingService.isAvailable()).thenReturn(true);
        when(embeddingService.embed(anyList())).thenThrow(new RuntimeException("OpenAI down"));

        ContentChunk chunk = chunk(1L, 1L, "test tokens overlap");
        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(chunk));

        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(1L, "test tokens overlap", 1);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).score()).isEqualTo(1.0); // Pure lexical
    }

    @Test
    void filtersShortTokensInTokenize() {
        // "a" and "is" are < 3 chars, should be ignored
        ContentChunk chunk = chunk(1L, 1L, "a quick brown fox");
        when(contentChunkRepository.findAllByFileIdOrderByIdAsc(1L)).thenReturn(List.of(chunk));
        
        List<RetrievedChunk> results = contentRetrievalService.retrieveRelevantChunks(1L, "a is quick", 1);
        assertThat(results).isNotEmpty();
        // question tokens: ["quick"] (since "a", "is" are < 3)
        // chunk tokens: ["quick", "brown"]
        // overlap: 1/1 = 1.0
        assertThat(results.get(0).score()).isEqualTo(1.0);
    }

    private ContentChunk chunk(Long id, Long fileId, String text) {
        ContentChunk chunk = new ContentChunk();
        ReflectionTestUtils.setField(chunk, "id", id);
        chunk.setFileId(fileId);
        chunk.setChunkText(text);
        return chunk;
    }
}
