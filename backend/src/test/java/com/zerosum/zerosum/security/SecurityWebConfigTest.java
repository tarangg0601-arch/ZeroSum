package com.zerosum.zerosum.security;

import com.zerosum.zerosum.controller.AuthController;
import com.zerosum.zerosum.dto.AuthResponse;
import com.zerosum.zerosum.dto.LoginRequest;
import com.zerosum.zerosum.repository.GroupMemberRepository;
import com.zerosum.zerosum.repository.UserRepository;
import com.zerosum.zerosum.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@EnableWebSecurity
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.auth.token-secret=web-config-test-secret-that-is-at-least-32-bytes")
class SecurityWebConfigTest {
    @Autowired MockMvc http;
    @MockitoBean AuthService authService;
    @MockitoBean GroupMemberRepository groupMembers;
    @MockitoBean UserRepository users;

    @Test
    void requiresBearerAuthenticationForApiRoutesButAllowsLogin() throws Exception {
        http.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        when(authService.login(any(LoginRequest.class), any())).thenReturn(new AuthResponse(1L, "A", "a@example.test", "signed"));
        http.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"email\":\"a@example.test\",\"password\":\"password\"}"))
                .andExpect(status().isOk());
    }
}
