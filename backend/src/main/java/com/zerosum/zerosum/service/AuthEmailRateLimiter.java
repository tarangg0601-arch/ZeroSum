package com.zerosum.zerosum.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AuthEmailRateLimiter {
    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WINDOW = Duration.ofHours(1);
    private final JdbcTemplate jdbc;

    public AuthEmailRateLimiter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean allow(String operation, String normalizedEmail, String clientAddress) {
        String bucket = digest(operation + "\n" + normalizedEmail + "\n" + clientAddress);
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);
        Integer attempts = jdbc.queryForObject("""
                INSERT INTO auth_email_attempts (bucket_hash, attempt_count, window_started_at)
                VALUES (?, 1, ?)
                ON CONFLICT (bucket_hash) DO UPDATE SET
                    attempt_count = CASE WHEN auth_email_attempts.window_started_at <= ? THEN 1 ELSE auth_email_attempts.attempt_count + 1 END,
                    window_started_at = CASE WHEN auth_email_attempts.window_started_at <= ? THEN EXCLUDED.window_started_at ELSE auth_email_attempts.window_started_at END
                RETURNING attempt_count
                """, Integer.class, bucket, Timestamp.from(now), Timestamp.from(cutoff), Timestamp.from(cutoff));
        jdbc.update("DELETE FROM auth_email_attempts WHERE window_started_at < ?", Timestamp.from(now.minus(Duration.ofDays(2))));
        return attempts != null && attempts <= MAX_ATTEMPTS;
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
