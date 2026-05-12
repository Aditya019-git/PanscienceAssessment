package com.panscience.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionAnswerRequest(
    @NotBlank(message = "Question must not be blank")
    String question
) {
}
