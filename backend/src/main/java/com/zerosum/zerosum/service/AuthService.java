package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.AuthResponse;
import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.dto.RegisterRequest;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder tokens;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder tokens,
                       LoginRateLimiter loginRateLimiter) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
        }

        User user = new User();
        user.setUsername(username);
        String displayName = request.name() == null ? "" : request.name().trim();
        user.setName(displayName.isEmpty() ? username : displayName);
        user.setEmail(null);
        user.setEmailVerified(false);
        user.setPasswordHash(passwords.encode(request.password()));
        try {
            return response(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException duplicate) {
            // The database's case-insensitive unique index is the final arbiter for concurrent registrations.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken");
        }
    }

    public AuthResponse login(LoginRequest request, String remoteAddress) {
        String username = normalizeUsername(request.username());
        String bucket = loginRateLimiter.bucket(username, remoteAddress);
        if (loginRateLimiter.isBlocked(bucket)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Try again later.");
        }
        User user = users.findByUsernameIgnoreCase(username).orElse(null);
        if (user == null || user.getPasswordHash() == null || !passwords.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(bucket);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        loginRateLimiter.clear(bucket);
        return response(user);
    }

    @Transactional
    public void logout(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        user.incrementAuthVersion();
        users.save(user);
    }

    private AuthResponse response(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("zerosum").subject(user.getId().toString())
                .issuedAt(now).expiresAt(now.plusSeconds(900)).id(UUID.randomUUID().toString())
                .claim("userId", user.getId()).claim("name", user.getName()).claim("ver", user.getAuthVersion()).build();
        String token = tokens.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(user.getId(), user.getName(), user.getUsername(), token);
    }

    static String normalizeUsername(String username) { return username.trim().toLowerCase(Locale.ROOT); }
}
