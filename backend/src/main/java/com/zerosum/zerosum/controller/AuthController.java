package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.AuthResponse;
import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.dto.RegisterRequest;
import com.zerosum.zerosum.dto.ActionResponse;
import com.zerosum.zerosum.dto.EmailRequest;
import com.zerosum.zerosum.dto.TokenRequest;
import com.zerosum.zerosum.dto.PasswordTokenRequest;
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
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ActionResponse register(@Valid @RequestBody RegisterRequest request) { return auth.register(request); }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        if (trustRenderClientIp) {
            String forwardedIp = servletRequest.getHeader("CF-Connecting-IP");
            if (TrustedClientAddress.isIpLiteral(forwardedIp)) clientIp = forwardedIp.trim();
        }
        return auth.login(request, clientIp);
    }

    @PostMapping("/verify-email")
    public ActionResponse verifyEmail(@Valid @RequestBody TokenRequest request) { return auth.verifyEmail(request.token()); }

    @PostMapping("/forgot-password")
    public ActionResponse forgotPassword(@Valid @RequestBody EmailRequest request) { return auth.requestPasswordReset(request.email()); }

    @PostMapping("/reset-password")
    public ActionResponse resetPassword(@Valid @RequestBody PasswordTokenRequest request) {
        return auth.resetPassword(request.token(), request.password());
    }

    @PostMapping("/claim-legacy-account")
    public ActionResponse claimLegacyAccount(@Valid @RequestBody EmailRequest request) { return auth.claimLegacyAccount(request.email()); }

    @PostMapping("/complete-legacy-claim")
    public ActionResponse completeLegacyClaim(@Valid @RequestBody PasswordTokenRequest request) {
        return auth.completeLegacyClaim(request.token(), request.password());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) { auth.logout(Long.valueOf(jwt.getSubject())); }
}
