package com.panscience.assessment.dto;

import com.panscience.assessment.entity.FileCategory;
import com.panscience.assessment.entity.ProcessingStatus;
import com.panscience.assessment.entity.StoredFile;
import java.time.Instant;

public record FileMetadataResponse(
    Long id,
    String originalName,
    String storedName,
    String contentType,
    FileCategory fileCategory,
    ProcessingStatus processingStatus,
    String storagePath,
    String summary,
    Instant createdAt
) {

    public static FileMetadataResponse from(StoredFile storedFile) {
        return new FileMetadataResponse(
            storedFile.getId(),
            storedFile.getOriginalName(),
            storedFile.getStoredName(),
            storedFile.getContentType(),
            storedFile.getFileCategory(),
            storedFile.getProcessingStatus(),
            storedFile.getStoragePath(),
            storedFile.getSummary(),
            storedFile.getCreatedAt()
        );
    }
}

