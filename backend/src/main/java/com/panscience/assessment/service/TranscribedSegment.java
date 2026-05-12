package com.panscience.assessment.service;

public record TranscribedSegment(
    String text,
    double startTime,
    double endTime,
    int sequenceNumber
) {
}

