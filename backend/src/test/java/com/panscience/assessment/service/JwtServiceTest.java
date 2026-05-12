package com.panscience.assessment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
            "test-secret-key-that-is-at-least-32-characters-long",
            86400000L
        );
    }

    @Test
    void generatesTokenAndExtractsUsername() {
        String token = jwtService.generateToken("user@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    void validatesTokenCorrectly() {
        String token = jwtService.generateToken("admin@example.com");

        assertThat(jwtService.isTokenValid(token, "admin@example.com")).isTrue();
    }

    @Test
    void rejectsTokenForWrongUsername() {
        String token = jwtService.generateToken("user@example.com");

        assertThat(jwtService.isTokenValid(token, "other@example.com")).isFalse();
    }

    @Test
    void rejectsExpiredToken() throws InterruptedException {
        JwtService shortLivedService = new JwtService(
            "test-secret-key-that-is-at-least-32-characters-long",
            1L // 1 ms expiration
        );

        String token = shortLivedService.generateToken("user@example.com");
        Thread.sleep(10);

        assertThat(shortLivedService.isTokenValid(token, "user@example.com")).isFalse();
    }

    @Test
    void generatesTokenWithExtraClaims() {
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("role", "admin");

        String token = jwtService.generateToken(claims, "admin@example.com");

        assertThat(jwtService.extractUsername(token)).isEqualTo("admin@example.com");
    }
}
