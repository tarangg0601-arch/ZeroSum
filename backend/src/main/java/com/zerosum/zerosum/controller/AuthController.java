package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.AuthResponse;
import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.dto.RegisterRequest;
import com.zerosum.zerosum.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final boolean trustRenderClientIp;

    public AuthController(AuthService auth,
                          @Value("${app.auth.trust-render-client-ip:false}") boolean trustRenderClientIp) {
        this.auth = auth;
        this.trustRenderClientIp = trustRenderClientIp;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return auth.login(request, clientAddress(servletRequest));
    }

    private String clientAddress(HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        if (trustRenderClientIp) {
            String forwardedIp = servletRequest.getHeader("CF-Connecting-IP");
            if (TrustedClientAddress.isIpLiteral(forwardedIp)) clientIp = forwardedIp.trim();
        }
        return clientIp;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) { auth.logout(Long.valueOf(jwt.getSubject())); }
}
