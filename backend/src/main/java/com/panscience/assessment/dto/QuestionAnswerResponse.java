package com.panscience.assessment.dto;

import java.util.List;

public record QuestionAnswerResponse(
    Long fileId,
    String question,
    String answer,
    Double suggestedPlaybackStartTime,
    List<AnswerSourceResponse> sources
) {
}
