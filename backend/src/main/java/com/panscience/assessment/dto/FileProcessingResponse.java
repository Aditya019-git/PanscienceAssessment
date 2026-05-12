package com.panscience.assessment.dto;

import com.panscience.assessment.entity.ProcessingStatus;

public record FileProcessingResponse(
    Long fileId,
    ProcessingStatus processingStatus,
    int chunkCount,
    int transcriptSegmentCount,
    String message
) {
}

