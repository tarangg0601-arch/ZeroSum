package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.dto.RegisterRequest;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.UserRepository;
import com.zerosum.zerosum.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void signsTokensWithConfiguredSecretAndRejectsShortSecrets() {
        SecurityConfig config = new SecurityConfig();
        var key = config.jwtSecretKey("unit-test-signing-key-that-is-at-least-32-bytes-long");
        JwtDecoder decoder = config.jwtDecoder(key);
        Instant now = Instant.now();
        String token = config.jwtEncoder(key).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("zerosum").subject("42").issuedAt(now).expiresAt(now.plusSeconds(60)).build())).getTokenValue();
        assertThrows(IllegalStateException.class, () -> config.jwtSecretKey("too-short"));
        assertTrue(key.getEncoded().length >= 32);
        assertEquals("42", decoder.decode(token).getSubject());
    }

    @Test
    void registrationStoresHashedPasswordAndRequiresEmailVerificationBeforeLogin() {
        var users = mock(UserRepository.class);
        var passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var encoder = mock(JwtEncoder.class);
        var tokenService = mock(AuthTokenService.class);
        var limiter = mock(LoginRateLimiter.class);
        var emailLimiter = mock(AuthEmailRateLimiter.class);
        var service = new AuthService(users, passwords, encoder, tokenService, limiter, emailLimiter);
        when(emailLimiter.allow("register", "person@example.test", "127.0.0.1")).thenReturn(true);
        when(users.findByEmailIgnoreCase("person@example.test")).thenReturn(Optional.empty());
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });
        assertNotNull(service.register(new RegisterRequest("Person", " Person@Example.Test ", "a-long-test-password-123"), "127.0.0.1").message());
        verify(users).save(argThat(user -> user.getPasswordHash() != null
                && !user.getPasswordHash().equals("a-long-test-password-123")
                && passwords.matches("a-long-test-password-123", user.getPasswordHash())
                && user.getEmail().equals("person@example.test") && !user.isEmailVerified()));
        verify(tokenService).send(any(User.class), eq(com.zerosum.zerosum.entity.AuthToken.Purpose.EMAIL_VERIFICATION));

        User account = account(passwords, false);
        when(limiter.bucket("person@example.test", "127.0.0.1")).thenReturn("bucket");
        when(limiter.isBlocked("bucket")).thenReturn(false);
        when(users.findByEmailIgnoreCase("person@example.test")).thenReturn(Optional.of(account));
        assertThrows(ResponseStatusException.class, () -> service.login(new LoginRequest("person@example.test", "a-long-test-password-123"), "127.0.0.1"));
        verify(limiter).recordFailure("bucket");
        verifyNoInteractions(encoder);
    }

    @Test
    void repeatedBadCredentialsRecordFailuresAndBlockedBucketsReturn429() {
        var users = mock(UserRepository.class);
        var passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var limiter = mock(LoginRateLimiter.class);
        var service = new AuthService(users, passwords, mock(JwtEncoder.class), mock(AuthTokenService.class), limiter, mock(AuthEmailRateLimiter.class));
        when(limiter.bucket("person@example.test", "127.0.0.1")).thenReturn("hashed-bucket");
        when(limiter.isBlocked("hashed-bucket")).thenReturn(false, false, false, false, false, true);
        when(users.findByEmailIgnoreCase("person@example.test")).thenReturn(Optional.of(account(passwords, true)));
        for (int i = 0; i < 5; i++) {
            assertThrows(ResponseStatusException.class, () -> service.login(new LoginRequest("person@example.test", "bad-password"), "127.0.0.1"));
        }
        var limited = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("person@example.test", "bad-password"), "127.0.0.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getStatusCode());
        verify(limiter, times(5)).recordFailure("hashed-bucket");
    }

    private User account(Pbkdf2PasswordEncoder passwords, boolean verified) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 42L);
        user.setName("Person"); user.setEmail("person@example.test"); user.setEmailVerified(verified);
        user.setPasswordHash(passwords.encode("a-long-test-password-123"));
        return user;
    }
}
