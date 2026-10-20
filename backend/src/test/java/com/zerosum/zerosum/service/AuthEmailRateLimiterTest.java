package com.zerosum.zerosum.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthEmailRateLimiterTest {
    @Test
    void permitsThreeRequestsPerEmailIpAndOperationWindowThenSuppressesFurtherRequests() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any()))
                .thenReturn(1, 2, 3, 4);
        AuthEmailRateLimiter limiter = new AuthEmailRateLimiter(jdbc);

        assertTrue(limiter.allow("password-reset", "person@example.test", "203.0.113.8"));
        assertTrue(limiter.allow("password-reset", "person@example.test", "203.0.113.8"));
        assertTrue(limiter.allow("password-reset", "person@example.test", "203.0.113.8"));
        assertFalse(limiter.allow("password-reset", "person@example.test", "203.0.113.8"));
        verify(jdbc, times(4)).update(anyString(), any(Timestamp.class));
    }

    @Test
    void hashesEmailAndAddressBeforeWritingBucket() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any())).thenReturn(1);
        AuthEmailRateLimiter limiter = new AuthEmailRateLimiter(jdbc);

        limiter.allow("register", "person@example.test", "203.0.113.8");
        verify(jdbc).queryForObject(anyString(), eq(Integer.class), argThat(value ->
                value instanceof String hash && hash.matches("[0-9a-f]{64}")
                        && !hash.contains("person") && !hash.contains("203.0.113.8")), any(), any(), any());
    }
}
