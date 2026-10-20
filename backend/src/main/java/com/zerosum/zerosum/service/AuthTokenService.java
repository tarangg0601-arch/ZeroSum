package com.zerosum.zerosum.service;

import com.zerosum.zerosum.entity.AuthToken;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.AuthTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthTokenService {
    private final AuthTokenRepository tokens;
    private final EmailSender email;
    private final String publicBaseUrl;
    private final SecureRandom random = new SecureRandom();

    public AuthTokenService(AuthTokenRepository tokens, EmailSender email,
                            @Value("${app.auth.email.public-base-url:http://localhost:5173}") String publicBaseUrl) {
        this.tokens = tokens;
        this.email = email;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public void send(User user, AuthToken.Purpose purpose) {
        Instant now = Instant.now();
        tokens.deleteOld(now.minus(Duration.ofDays(30)));
        tokens.deleteActive(user.getId(), purpose);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthToken record = new AuthToken();
        record.setUser(user);
        record.setPurpose(purpose);
        record.setTokenHash(hash(raw));
        record.setExpiresAt(now.plus(purpose == AuthToken.Purpose.EMAIL_VERIFICATION ? Duration.ofHours(24) : Duration.ofMinutes(30)));
        tokens.save(record);

        String route = switch (purpose) {
            case EMAIL_VERIFICATION -> "/verify-email";
            case PASSWORD_RESET -> "/reset-password";
            case LEGACY_CLAIM -> "/claim-account";
        };
        String subject = switch (purpose) {
            case EMAIL_VERIFICATION -> "Verify your ZeroSum email";
            case PASSWORD_RESET -> "Reset your ZeroSum password";
            case LEGACY_CLAIM -> "Secure your existing ZeroSum account";
        };
        String action = switch (purpose) {
            case EMAIL_VERIFICATION -> "Verify your email address";
            case PASSWORD_RESET -> "Reset your password";
            case LEGACY_CLAIM -> "Set a password and link your existing account";
        };
        email.send(user.getEmail(), subject, action + " using this one-time link (expires soon):\n\n"
                + publicBaseUrl + route + "#token=" + raw + "\n\nIf you did not request this, ignore this message.");
    }

    @Transactional
    public User consume(String rawToken, AuthToken.Purpose purpose) {
        if (rawToken == null || rawToken.isBlank()) throw invalidToken();
        String tokenHash = hash(rawToken);
        Instant now = Instant.now();
        if (tokens.consume(tokenHash, purpose, now) != 1) throw invalidToken();
        return tokens.findByTokenHash(tokenHash).map(AuthToken::getUser).orElseThrow(AuthTokenService::invalidToken);
    }

    static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or expired");
    }
}
