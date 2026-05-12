package com.panscience.assessment.service;

import java.util.List;

public record TranscriptionResult(
    String transcriptText,
    List<TranscribedSegment> segments
) {
}

