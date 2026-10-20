package com.zerosum.zerosum.security;

import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class TokenVersionFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public TokenVersionFilter(UserRepository users) { this.users = users; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            User user;
            try { user = users.findById(Long.valueOf(jwt.getName())).orElse(null); }
            catch (NumberFormatException invalidSubject) { user = null; }
            Number version = jwt.getToken().getClaim("ver");
            if (user == null || version == null || version.intValue() != user.getAuthVersion()) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
