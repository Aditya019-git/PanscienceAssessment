package com.panscience.assessment.dto;

import com.panscience.assessment.entity.ProcessingStatus;

public record FileSummaryResponse(
    Long fileId,
    ProcessingStatus processingStatus,
    String summary
) {
}
