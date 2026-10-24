package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.AuthResponse;
import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerClientAddressTest {
    private final LoginRequest request = new LoginRequest("person_1", "not-logged");

    @Test
    void usesRenderClientIpOnlyWhenRenderProxyTrustIsEnabled() {
        AuthService auth = mock(AuthService.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.4");
        when(servletRequest.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.7");

        new AuthController(auth, true).login(request, servletRequest);

        verify(auth).login(any(LoginRequest.class), eq("203.0.113.7"));
    }

    @Test
    void ignoresForwardedClientIpWhenProxyTrustIsDisabledOrHeaderIsInvalid() {
        AuthService auth = mock(AuthService.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getRemoteAddr()).thenReturn("10.0.0.4");
        when(servletRequest.getHeader("CF-Connecting-IP")).thenReturn("spoof.example");

        new AuthController(auth, true).login(request, servletRequest);
        verify(auth).login(any(LoginRequest.class), eq("10.0.0.4"));

        new AuthController(auth, false).login(request, servletRequest);
        verify(auth, org.mockito.Mockito.times(2)).login(any(LoginRequest.class), eq("10.0.0.4"));
    }
}
