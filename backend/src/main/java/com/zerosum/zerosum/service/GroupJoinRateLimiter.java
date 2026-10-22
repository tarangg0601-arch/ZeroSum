package com.zerosum.zerosum.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class GroupJoinRateLimiter {
    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final JdbcTemplate jdbc;

    public GroupJoinRateLimiter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean check(String clientAddress) {
        try {
            String bucket = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(("group-join\n" + clientAddress).getBytes(StandardCharsets.UTF_8)));
            Instant now = Instant.now();
            Integer attempts = jdbc.queryForObject("""
                    INSERT INTO group_join_attempts (bucket_hash, attempt_count, window_started_at)
                    VALUES (?, 1, ?)
                    ON CONFLICT (bucket_hash) DO UPDATE SET
                        attempt_count = CASE WHEN group_join_attempts.window_started_at <= ? THEN 1 ELSE group_join_attempts.attempt_count + 1 END,
                        window_started_at = CASE WHEN group_join_attempts.window_started_at <= ? THEN EXCLUDED.window_started_at ELSE group_join_attempts.window_started_at END
                    RETURNING attempt_count
                    """, Integer.class, bucket, Timestamp.from(now), Timestamp.from(now.minus(WINDOW)), Timestamp.from(now.minus(WINDOW)));
            return attempts != null && attempts <= MAX_ATTEMPTS;
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
