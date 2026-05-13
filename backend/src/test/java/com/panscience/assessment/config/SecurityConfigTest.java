package com.panscience.assessment.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

    @Test
    void passwordEncoder_ReturnsBCryptPasswordEncoder() {
        SecurityConfig config = new SecurityConfig(null);
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        assertThat(encoder.encode("password")).isNotEqualTo("password");
    }

    @Test
    void corsConfigurationSource_ReturnsValidSource() {
        SecurityConfig config = new SecurityConfig(null);
        CorsConfigurationSource source = config.corsConfigurationSource();
        assertNotNull(source);
    }
}
