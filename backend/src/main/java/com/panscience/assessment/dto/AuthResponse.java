package com.panscience.assessment.dto;

public record AuthResponse(
    String token,
    String email
) {
}
