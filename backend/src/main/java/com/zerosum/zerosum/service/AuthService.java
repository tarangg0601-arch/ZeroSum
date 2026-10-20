package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.ActionResponse;
import com.zerosum.zerosum.dto.AuthResponse;
import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.dto.RegisterRequest;
import com.zerosum.zerosum.entity.AuthToken;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.UserRepository;
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
    private static final String GENERIC_ACCOUNT_MESSAGE = "If the account is eligible, you will receive an email with next steps.";
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder tokens;
    private final AuthTokenService authTokens;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder tokens,
                       AuthTokenService authTokens, LoginRateLimiter loginRateLimiter) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.authTokens = authTokens;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional
    public ActionResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        User existing = users.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null) {
            if (existing.getPasswordHash() == null) authTokens.send(existing, AuthToken.Purpose.LEGACY_CLAIM);
            else if (!existing.isEmailVerified()) authTokens.send(existing, AuthToken.Purpose.EMAIL_VERIFICATION);
            return new ActionResponse(GENERIC_ACCOUNT_MESSAGE);
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(request.password()));
        user.setEmailVerified(false);
        user = users.save(user);
        authTokens.send(user, AuthToken.Purpose.EMAIL_VERIFICATION);
        return new ActionResponse(GENERIC_ACCOUNT_MESSAGE);
    }

    public AuthResponse login(LoginRequest request, String remoteAddress) {
        String email = normalizeEmail(request.email());
        String bucket = loginRateLimiter.bucket(email, remoteAddress);
        if (loginRateLimiter.isBlocked(bucket)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Try again later.");
        }
        User user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || user.getPasswordHash() == null || !user.isEmailVerified()
                || !passwords.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(bucket);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        loginRateLimiter.clear(bucket);
        return response(user);
    }

    @Transactional
    public ActionResponse verifyEmail(String rawToken) {
        User user = authTokens.consume(rawToken, AuthToken.Purpose.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        users.save(user);
        return new ActionResponse("Email verified. You can now sign in.");
    }

    @Transactional
    public ActionResponse requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(User::isEmailVerified)
                .ifPresent(user -> authTokens.send(user, AuthToken.Purpose.PASSWORD_RESET));
        return new ActionResponse(GENERIC_ACCOUNT_MESSAGE);
    }

    @Transactional
    public ActionResponse resetPassword(String rawToken, String newPassword) {
        User user = authTokens.consume(rawToken, AuthToken.Purpose.PASSWORD_RESET);
        user.setPasswordHash(passwords.encode(newPassword));
        user.incrementAuthVersion();
        users.save(user);
        return new ActionResponse("Password updated. Sign in with your new password.");
    }

    @Transactional
    public ActionResponse claimLegacyAccount(String email) {
        users.findByEmailIgnoreCase(normalizeEmail(email))
                .filter(user -> user.getPasswordHash() == null)
                .ifPresent(user -> authTokens.send(user, AuthToken.Purpose.LEGACY_CLAIM));
        return new ActionResponse(GENERIC_ACCOUNT_MESSAGE);
    }

    @Transactional
    public ActionResponse completeLegacyClaim(String rawToken, String newPassword) {
        User user = authTokens.consume(rawToken, AuthToken.Purpose.LEGACY_CLAIM);
        if (user.getPasswordHash() != null || user.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This link is invalid or expired");
        }
        user.setPasswordHash(passwords.encode(newPassword));
        user.setEmailVerified(true);
        user.incrementAuthVersion();
        users.save(user);
        return new ActionResponse("Account secured. You can now sign in.");
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
        return new AuthResponse(user.getId(), user.getName(), user.getEmail(), token);
    }

    static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
