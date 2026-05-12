package com.panscience.assessment.config;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panscience.assessment.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        filter = new JwtAuthenticationFilter(jwtService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void proceedsWhenNoAuthHeader() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void proceedsWhenHeaderDoesNotStartWithBearer() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic YWxhZGRpbjpvcGVuc2VzYW1l");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).extractUsername(anyString());
    }

    @Test
    void setsAuthenticationWhenTokenValid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-jwt");
        when(jwtService.extractUsername("valid-jwt")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("valid-jwt", "user@example.com")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        org.junit.jupiter.api.Assertions.assertNotNull(auth);
        org.junit.jupiter.api.Assertions.assertEquals("user@example.com", auth.getName());
    }

    @Test
    void doesNotSetAuthenticationWhenTokenInvalid() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-jwt");
        when(jwtService.extractUsername("invalid-jwt")).thenReturn("user@example.com");
        when(jwtService.isTokenValid("invalid-jwt", "user@example.com")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        org.junit.jupiter.api.Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void handlesJwtServiceExceptions() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer broken-jwt");
        when(jwtService.extractUsername("broken-jwt")).thenThrow(new RuntimeException("parse error"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        org.junit.jupiter.api.Assertions.assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
