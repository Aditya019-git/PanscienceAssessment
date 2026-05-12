package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.panscience.assessment.dto.QuestionAnswerResponse;
import com.panscience.assessment.entity.ContentChunk;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import com.panscience.assessment.exception.FileNotReadyException;
import com.panscience.assessment.repository.StoredFileRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class QuestionAnswerServiceTest {

    @Mock
    private StoredFileRepository storedFileRepository;

    @Mock
    private ContentRetrievalService contentRetrievalService;

    @Mock
    private AnswerGenerationService answerGenerationService;

    private QuestionAnswerService questionAnswerService;

    @BeforeEach
    void setUp() {
        questionAnswerService = new QuestionAnswerService(
            storedFileRepository,
            contentRetrievalService,
            answerGenerationService,
            5
        );
    }

    @Test
    void returnsGroundedAnswerAndSuggestedPlaybackTimeForMedia() {
        StoredFile storedFile = readyFile(1L, FileCategory.VIDEO);
        ContentChunk chunk = chunk(10L, "The product demo starts with the upload workflow.", 42.5, 56.0);

        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(storedFile));
        when(contentRetrievalService.retrieveRelevantChunks(1L, "When does the upload demo start?", 5))
            .thenReturn(List.of(new RetrievedChunk(chunk, 0.91)));
        when(answerGenerationService.isAvailable()).thenReturn(true);
        when(answerGenerationService.generateAnswer(storedFile, "When does the upload demo start?", List.of(new RetrievedChunk(chunk, 0.91))))
            .thenReturn("The upload demo starts around 42.5 seconds [1].");

        QuestionAnswerResponse response = questionAnswerService.answerQuestion(
            1L,
            "When does the upload demo start?"
        );

        assertThat(response.answer()).contains("42.5 seconds");
        assertThat(response.suggestedPlaybackStartTime()).isEqualTo(42.5);
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).startTime()).isEqualTo(42.5);
    }

    @Test
    void fallsBackWhenAnswerGenerationIsUnavailable() {
        StoredFile storedFile = readyFile(1L, FileCategory.PDF);
        ContentChunk chunk = chunk(10L, "The platform supports chunk-based retrieval for question answering.", null, null);

        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(storedFile));
        when(contentRetrievalService.retrieveRelevantChunks(1L, "How does retrieval work?", 5))
            .thenReturn(List.of(new RetrievedChunk(chunk, 0.73)));
        when(answerGenerationService.isAvailable()).thenReturn(false);

        QuestionAnswerResponse response = questionAnswerService.answerQuestion(1L, "How does retrieval work?");

        assertThat(response.answer()).contains("not configured yet");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).excerpt()).contains("chunk-based retrieval");
    }

    @Test
    void rejectsQuestionsBeforeFileProcessingCompletes() {
        StoredFile storedFile = readyFile(1L, FileCategory.PDF);
        storedFile.setProcessingStatus(ProcessingStatus.PROCESSING);

        when(storedFileRepository.findById(1L)).thenReturn(Optional.of(storedFile));

        assertThatThrownBy(() -> questionAnswerService.answerQuestion(1L, "What is inside this file?"))
            .isInstanceOf(FileNotReadyException.class)
            .hasMessageContaining("PROCESSING");
    }

    private StoredFile readyFile(Long id, FileCategory fileCategory) {
        StoredFile storedFile = new StoredFile();
        ReflectionTestUtils.setField(storedFile, "id", id);
        storedFile.setOriginalName("demo-file");
        storedFile.setFileCategory(fileCategory);
        storedFile.setProcessingStatus(ProcessingStatus.READY);
        storedFile.setStoragePath("uploads/demo-file");
        storedFile.setContentType("application/octet-stream");
        storedFile.setStoredName("demo-file");
        return storedFile;
    }

    private ContentChunk chunk(Long id, String text, Double startTime, Double endTime) {
        ContentChunk chunk = new ContentChunk();
        ReflectionTestUtils.setField(chunk, "id", id);
        chunk.setFileId(1L);
        chunk.setChunkText(text);
        chunk.setStartTime(startTime);
        chunk.setEndTime(endTime);
        return chunk;
    }
}
