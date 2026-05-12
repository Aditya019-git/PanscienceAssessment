package com.panscience.assessment.service;

import com.panscience.assessment.entity.StoredFile;
import java.util.List;
import reactor.core.publisher.Flux;

public interface AnswerGenerationService {

    boolean isAvailable();

    String generateAnswer(StoredFile storedFile, String question, List<RetrievedChunk> retrievedChunks);

    Flux<String> streamAnswer(StoredFile storedFile, String question, List<RetrievedChunk> retrievedChunks);
}
