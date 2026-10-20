package com.zerosum.zerosum.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;

@Service
public class LoginRateLimiter {
    private static final int MAX_FAILURES = 5;
    private final JdbcTemplate jdbc;

    public LoginRateLimiter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public String bucket(String normalizedEmail, String remoteAddress) {
        String raw = normalizedEmail + "\n" + (remoteAddress == null ? "unknown" : remoteAddress);
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    public boolean isBlocked(String bucket) {
        Integer count = jdbc.queryForObject("select coalesce((select attempt_count from auth_login_attempts where bucket_hash = ? and window_started_at > current_timestamp - interval '15 minutes'), 0)", Integer.class, bucket);
        return count != null && count >= MAX_FAILURES;
    }

    public void recordFailure(String bucket) {
        jdbc.update("""
                insert into auth_login_attempts(bucket_hash, attempt_count, window_started_at)
                values (?, 1, current_timestamp)
                on conflict (bucket_hash) do update set
                    attempt_count = case when auth_login_attempts.window_started_at <= current_timestamp - interval '15 minutes' then 1 else auth_login_attempts.attempt_count + 1 end,
                    window_started_at = case when auth_login_attempts.window_started_at <= current_timestamp - interval '15 minutes' then current_timestamp else auth_login_attempts.window_started_at end
                """, bucket);
        jdbc.update("delete from auth_login_attempts where window_started_at < ?", Timestamp.from(Instant.now().minusSeconds(86400)));
    }

    public void clear(String bucket) { jdbc.update("delete from auth_login_attempts where bucket_hash = ?", bucket); }
}
