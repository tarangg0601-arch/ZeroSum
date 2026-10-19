package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.dto.RegisterRequest;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.UserRepository;
import com.zerosum.zerosum.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @Test
    void signsTokensWithTheConfiguredSecretAndRejectsShortSecrets() {
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
    void registrationStoresOnlyPasswordHashAndLoginIssuesShortLivedToken() {
        UserRepository users = mock(UserRepository.class);
        Pbkdf2PasswordEncoder passwords = Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
        JwtEncoder tokens = mock(JwtEncoder.class);
        AuthService service = new AuthService(users, passwords, tokens);
        when(users.existsByEmailIgnoreCase("person@example.test")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 42L);
            return user;
        });
        Instant now = Instant.now();
        when(tokens.encode(any(JwtEncoderParameters.class))).thenReturn(Jwt.withTokenValue("signed-test-token")
                .header("alg", "HS256").issuer("zerosum").subject("42").issuedAt(now).expiresAt(now.plusSeconds(900))
                .claim("userId", 42L).claim("name", "Person").build());

        var registered = service.register(new RegisterRequest("Person", " Person@Example.Test ", "a-long-test-password-123"));
        assertEquals("signed-test-token", registered.token());
        verify(users).save(argThat(user -> user.getPasswordHash() != null
                && !user.getPasswordHash().equals("a-long-test-password-123")
                && passwords.matches("a-long-test-password-123", user.getPasswordHash())
                && user.getEmail().equals("person@example.test")));

        User account = new User();
        ReflectionTestUtils.setField(account, "id", 42L);
        account.setName("Person");
        account.setEmail("person@example.test");
        account.setPasswordHash(passwords.encode("a-long-test-password-123"));
        when(users.findByEmailIgnoreCase("person@example.test")).thenReturn(Optional.of(account));
        assertEquals(42L, service.login(new LoginRequest("PERSON@example.test", "a-long-test-password-123")).userId());
        assertThrows(RuntimeException.class, () -> service.login(new LoginRequest("person@example.test", "wrong-password")));
    }
}
