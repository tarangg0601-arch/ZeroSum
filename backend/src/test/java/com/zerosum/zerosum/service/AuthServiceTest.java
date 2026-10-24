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
        String token = config.jwtEncoder(key).encode(JwtEncoderParameters.from(JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),
                JwtClaimsSet.builder().issuer("zerosum").subject("42").issuedAt(now).expiresAt(now.plusSeconds(60)).build())).getTokenValue();
        assertThrows(IllegalStateException.class, () -> config.jwtSecretKey("too-short"));
        assertTrue(key.getEncoded().length >= 32);
        assertEquals("42", decoder.decode(token).getSubject());
    }

    @Test
    void registrationNormalizesUsernameHashesPasswordAndCreatesActiveAccountWithoutEmail() {
        var users = mock(UserRepository.class);
        var passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var encoder = mock(JwtEncoder.class);
        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("signed-token");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        var limiter = mock(LoginRateLimiter.class);
        var service = new AuthService(users, passwords, encoder, limiter);
        when(users.existsByUsernameIgnoreCase("person_1")).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });

        var result = service.register(new RegisterRequest(" Person_1 ", "  Person  ", "a-long-test-password-123"));

        assertEquals("person_1", result.username());
        assertEquals("signed-token", result.token());
        verify(users).saveAndFlush(argThat(user -> user.getUsername().equals("person_1")
                && user.getEmail() == null && !user.isEmailVerified()
                && user.getName().equals("Person")
                && passwords.matches("a-long-test-password-123", user.getPasswordHash())
                && !user.getPasswordHash().equals("a-long-test-password-123")));
        verifyNoInteractions(limiter);
    }

    @Test
    void registrationRejectsCaseInsensitiveDuplicateUsername() {
        var users = mock(UserRepository.class);
        var service = new AuthService(users, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
                mock(JwtEncoder.class), mock(LoginRateLimiter.class));
        when(users.existsByUsernameIgnoreCase("taken")).thenReturn(true);

        var conflict = assertThrows(ResponseStatusException.class,
                () -> service.register(new RegisterRequest("Taken", null, "a-long-test-password-123")));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void loginIsCaseInsensitiveAndRateLimitedAfterRepeatedInvalidPasswords() {
        var users = mock(UserRepository.class);
        var passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        var limiter = mock(LoginRateLimiter.class);
        var encoder = mock(JwtEncoder.class);
        var jwt = mock(Jwt.class);
        when(jwt.getTokenValue()).thenReturn("signed-token");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
        var service = new AuthService(users, passwords, encoder, limiter);
        User account = new User();
        ReflectionTestUtils.setField(account, "id", 42L);
        account.setUsername("person_1");
        account.setName("Person");
        account.setPasswordHash(passwords.encode("correct-password-123"));
        when(limiter.bucket("person_1", "127.0.0.1")).thenReturn("hashed-bucket");
        when(limiter.isBlocked("hashed-bucket")).thenReturn(false, false, false, false, false, false, true);
        when(users.findByUsernameIgnoreCase("person_1")).thenReturn(Optional.of(account));

        var login = service.login(new LoginRequest("PERSON_1", "correct-password-123"), "127.0.0.1");
        assertEquals("person_1", login.username());
        for (int i = 0; i < 5; i++) {
            assertThrows(ResponseStatusException.class,
                    () -> service.login(new LoginRequest("PERSON_1", "bad-password"), "127.0.0.1"));
        }
        var limited = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("PERSON_1", "bad-password"), "127.0.0.1"));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getStatusCode());
        verify(limiter, times(5)).recordFailure("hashed-bucket");
    }

    @Test
    void userWithoutPasswordCannotAuthenticateAndWrongPasswordIsGeneric() {
        var users = mock(UserRepository.class);
        var limiter = mock(LoginRateLimiter.class);
        var service = new AuthService(users, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(), mock(JwtEncoder.class), limiter);
        when(limiter.bucket("legacy_user", "127.0.0.1")).thenReturn("bucket");
        when(users.findByUsernameIgnoreCase("legacy_user")).thenReturn(Optional.of(new User()));

        var failure = assertThrows(ResponseStatusException.class,
                () -> service.login(new LoginRequest("legacy_user", "anything"), "127.0.0.1"));
        assertEquals(HttpStatus.UNAUTHORIZED, failure.getStatusCode());
        assertEquals("Invalid username or password", failure.getReason());
        verify(limiter).recordFailure("bucket");
    }
}
