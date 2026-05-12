package com.panscience.assessment.dto;

import com.panscience.assessment.entity.TranscriptSegment;

public record TranscriptSegmentResponse(
    Long id,
    String segmentText,
    Double startTime,
    Double endTime,
    Integer sequenceNumber
) {

    public static TranscriptSegmentResponse from(TranscriptSegment transcriptSegment) {
        return new TranscriptSegmentResponse(
            transcriptSegment.getId(),
            transcriptSegment.getSegmentText(),
            transcriptSegment.getStartTime(),
            transcriptSegment.getEndTime(),
            transcriptSegment.getSequenceNumber()
        );
    }
}

