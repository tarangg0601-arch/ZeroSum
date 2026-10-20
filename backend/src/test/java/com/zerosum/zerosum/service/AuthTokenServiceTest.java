package com.zerosum.zerosum.service;

import com.zerosum.zerosum.entity.AuthToken;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.AuthTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthTokenServiceTest {
    @Test
    void emailsRawTokenButPersistsOnlyHashAndReplacesPriorActiveToken() {
        AuthTokenRepository repository = mock(AuthTokenRepository.class);
        EmailSender sender = mock(EmailSender.class);
        AuthTokenService service = new AuthTokenService(repository, sender, "https://app.example.test/");
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 5L);
        user.setEmail("person@example.test");
        service.send(user, AuthToken.Purpose.EMAIL_VERIFICATION);

        var record = org.mockito.ArgumentCaptor.forClass(AuthToken.class);
        verify(repository).deleteActive(5L, AuthToken.Purpose.EMAIL_VERIFICATION);
        verify(repository).save(record.capture());
        var message = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(sender).send(eq("person@example.test"), eq("Verify your ZeroSum email"), message.capture());
        String rawToken = message.getValue().substring(message.getValue().indexOf("#token=") + 7).split("\\s", 2)[0];
        assertEquals(AuthTokenService.hash(rawToken), record.getValue().getTokenHash());
        assertNotEquals(rawToken, record.getValue().getTokenHash());
        assertEquals("https://app.example.test/verify-email#token=" + rawToken,
                message.getValue().split("\\n\\n")[1]);
        assertTrue(record.getValue().getExpiresAt().isAfter(Instant.now().plusSeconds(23 * 60 * 60)));
    }

    @Test
    void expiredOrAlreadyConsumedTokenIsRejectedAndSingleUse() {
        AuthTokenRepository repository = mock(AuthTokenRepository.class);
        AuthTokenService service = new AuthTokenService(repository, mock(EmailSender.class), "https://app.example.test");
        when(repository.consume(any(), eq(AuthToken.Purpose.PASSWORD_RESET), any())).thenReturn(0);
        var expired = assertThrows(ResponseStatusException.class,
                () -> service.consume("expired-secret", AuthToken.Purpose.PASSWORD_RESET));
        assertEquals(HttpStatus.BAD_REQUEST, expired.getStatusCode());

        when(repository.consume(any(), eq(AuthToken.Purpose.EMAIL_VERIFICATION), any())).thenReturn(1, 0);
        AuthToken token = new AuthToken();
        User owner = new User();
        ReflectionTestUtils.setField(owner, "id", 9L);
        token.setUser(owner);
        when(repository.findByTokenHash(AuthTokenService.hash("one-time"))).thenReturn(Optional.of(token));
        assertEquals(owner, service.consume("one-time", AuthToken.Purpose.EMAIL_VERIFICATION));
        var reused = assertThrows(ResponseStatusException.class,
                () -> service.consume("one-time", AuthToken.Purpose.EMAIL_VERIFICATION));
        assertEquals(HttpStatus.BAD_REQUEST, reused.getStatusCode());
    }
}
