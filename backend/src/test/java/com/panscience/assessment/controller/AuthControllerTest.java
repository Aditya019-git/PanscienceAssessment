package com.panscience.assessment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panscience.assessment.dto.AuthResponse;
import com.panscience.assessment.entity.User;
import com.panscience.assessment.exception.GlobalExceptionHandler;
import com.panscience.assessment.repository.UserRepository;
import com.panscience.assessment.service.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    @MockBean
    private com.panscience.assessment.config.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.panscience.assessment.config.RateLimitingFilter rateLimitingFilter;

    @Test
    void registersNewUserSuccessfully() throws Exception {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken("new@example.com")).thenReturn("jwt-token-abc");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"new@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-token-abc"))
            .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void rejectsRegistrationWhenEmailAlreadyExists() throws Exception {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"existing@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void logsInWithValidCredentials() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("user@example.com")).thenReturn("jwt-login-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("jwt-login-token"))
            .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    void rejectsLoginWithWrongPassword() throws Exception {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashed-password");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpassword", "hashed-password")).thenReturn(false);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@example.com\",\"password\":\"wrongpassword\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginForUnknownEmail() throws Exception {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nobody@example.com\",\"password\":\"password123\"}"))
            .andExpect(status().isUnauthorized());
    }
}
