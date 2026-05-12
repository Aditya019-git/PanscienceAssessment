package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.repository.ChunkEmbeddingRepository;
import com.panscience.assessment.repository.ContentChunkRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

    private ContentRetrievalService contentRetrievalService;

    @BeforeEach
    void setUp() {
        contentRetrievalService = new ContentRetrievalService(
            contentChunkRepository,
            chunkEmbeddingRepository,
            embeddingService,
            new ObjectMapper()
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

    private ContentChunk chunk(Long id, Long fileId, String text) {
        ContentChunk chunk = new ContentChunk();
        ReflectionTestUtils.setField(chunk, "id", id);
        chunk.setFileId(fileId);
        chunk.setChunkText(text);
        return chunk;
    }
}
