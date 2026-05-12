package com.panscience.assessment.config;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;

class RateLimitingFilterTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RateLimitingFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        filter = new RateLimitingFilter(redisTemplate);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void allowsRequestWhenUnderLimit() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(valueOperations.increment("rate_limit:127.0.0.1")).thenReturn(10L);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    void setsExpirationOnFirstRequest() throws Exception {
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(valueOperations.increment("rate_limit:192.168.1.1")).thenReturn(1L);

        filter.doFilterInternal(request, response, filterChain);

        verify(redisTemplate).expire("rate_limit:192.168.1.1", 1, TimeUnit.MINUTES);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void blocksRequestWhenOverLimit() throws Exception {
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(valueOperations.increment("rate_limit:10.0.0.1")).thenReturn(61L);
        
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(filterChain, never()).doFilter(request, response);
        org.junit.jupiter.api.Assertions.assertTrue(stringWriter.toString().contains("Too many requests"));
    }

    @Test
    void allowsRequestWhenCountIsNull() throws Exception {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(valueOperations.increment("rate_limit:127.0.0.1")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), eq(TimeUnit.MINUTES));
    }
}
