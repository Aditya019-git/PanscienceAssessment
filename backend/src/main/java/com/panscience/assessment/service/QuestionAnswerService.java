package com.panscience.assessment.service;

import com.panscience.assessment.dto.AnswerSourceResponse;
import com.panscience.assessment.dto.QuestionAnswerResponse;
import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import com.panscience.assessment.exception.FileNotReadyException;
import com.panscience.assessment.exception.StoredFileNotFoundException;
import com.panscience.assessment.repository.StoredFileRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;

@Service
public class QuestionAnswerService {

    private final StoredFileRepository storedFileRepository;
    private final ContentRetrievalService contentRetrievalService;
    private final AnswerGenerationService answerGenerationService;
    private final int topK;

    public QuestionAnswerService(
        StoredFileRepository storedFileRepository,
        ContentRetrievalService contentRetrievalService,
        AnswerGenerationService answerGenerationService,
        @Value("${app.retrieval.top-k:5}") int topK
    ) {
        this.storedFileRepository = storedFileRepository;
        this.contentRetrievalService = contentRetrievalService;
        this.answerGenerationService = answerGenerationService;
        this.topK = topK;
    }

    @Transactional
    public QuestionAnswerResponse answerQuestion(Long fileId, String question) {
        StoredFile storedFile = findStoredFile(fileId);

        if (storedFile.getProcessingStatus() != ProcessingStatus.READY) {
            throw new FileNotReadyException(fileId, storedFile.getProcessingStatus());
        }

        String normalizedQuestion = question == null ? "" : question.trim();
        List<RetrievedChunk> retrievedChunks = contentRetrievalService.retrieveRelevantChunks(
            fileId,
            normalizedQuestion,
            topK
        );

        List<AnswerSourceResponse> sources = toSourceResponses(retrievedChunks);
        Double suggestedPlaybackStartTime = suggestedPlaybackStartTime(storedFile, retrievedChunks);

        if (retrievedChunks.isEmpty()) {
            return new QuestionAnswerResponse(
                fileId,
                normalizedQuestion,
                "I could not find relevant content in the uploaded file for that question.",
                suggestedPlaybackStartTime,
                sources
            );
        }

        String answer = buildAnswer(storedFile, normalizedQuestion, retrievedChunks, sources);
        return new QuestionAnswerResponse(
            fileId,
            normalizedQuestion,
            answer,
            suggestedPlaybackStartTime,
            sources
        );
    }

    @Transactional(readOnly = true)
    public Flux<String> streamAnswer(Long fileId, String question) {
        StoredFile storedFile = findStoredFile(fileId);

        if (storedFile.getProcessingStatus() != ProcessingStatus.READY) {
            return Flux.error(new FileNotReadyException(fileId, storedFile.getProcessingStatus()));
        }

        String normalizedQuestion = question == null ? "" : question.trim();
        List<RetrievedChunk> retrievedChunks = contentRetrievalService.retrieveRelevantChunks(
            fileId,
            normalizedQuestion,
            topK
        );

        if (retrievedChunks.isEmpty()) {
            return Flux.just("I could not find relevant content in the uploaded file for that question.");
        }

        if (answerGenerationService.isAvailable()) {
            return answerGenerationService.streamAnswer(storedFile, normalizedQuestion, retrievedChunks);
        }

        return Flux.just(fallbackAnswer(toSourceResponses(retrievedChunks), false));
    }

    private StoredFile findStoredFile(Long fileId) {
        return storedFileRepository.findById(fileId)
            .orElseThrow(() -> new StoredFileNotFoundException(fileId));
    }

    private String buildAnswer(
        StoredFile storedFile,
        String question,
        List<RetrievedChunk> retrievedChunks,
        List<AnswerSourceResponse> sources
    ) {
        if (answerGenerationService.isAvailable()) {
            try {
                return answerGenerationService.generateAnswer(storedFile, question, retrievedChunks);
            } catch (RuntimeException ignored) {
                return fallbackAnswer(sources, true);
            }
        }

        return fallbackAnswer(sources, false);
    }

    private String fallbackAnswer(List<AnswerSourceResponse> sources, boolean modelFailed) {
        StringBuilder builder = new StringBuilder();
        if (modelFailed) {
            builder.append("I could not generate a model-based answer right now, but these excerpts are the strongest matches:\n");
        } else {
            builder.append("OpenAI answer generation is not configured yet, but these excerpts are the strongest matches:\n");
        }

        for (AnswerSourceResponse source : sources) {
            builder.append('[')
                .append(source.sourceNumber())
                .append("] ")
                .append(source.excerpt())
                .append('\n');
        }

        return builder.toString().trim();
    }

    private List<AnswerSourceResponse> toSourceResponses(List<RetrievedChunk> retrievedChunks) {
        return java.util.stream.IntStream.range(0, retrievedChunks.size())
            .mapToObj(index -> AnswerSourceResponse.from(index + 1, retrievedChunks.get(index)))
            .toList();
    }

    private Double suggestedPlaybackStartTime(StoredFile storedFile, List<RetrievedChunk> retrievedChunks) {
        if (storedFile.getFileCategory() != FileCategory.AUDIO && storedFile.getFileCategory() != FileCategory.VIDEO) {
            return null;
        }

        return retrievedChunks.stream()
            .map(retrievedChunk -> retrievedChunk.chunk().getStartTime())
            .filter(value -> value != null && value >= 0.0)
            .findFirst()
            .orElse(null);
    }
}
