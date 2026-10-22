package com.zerosum.zerosum.security;

import com.zerosum.zerosum.repository.GroupMemberRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.util.UriUtils;

public class GroupMembershipFilter extends OncePerRequestFilter {
    private final GroupMemberRepository members;
    public GroupMembershipFilter(GroupMemberRepository members) { this.members = members; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path;
        try {
            path = request.getRequestURI();
            for (int depth = 0; depth < 4; depth++) {
                String decoded = UriUtils.decode(path, StandardCharsets.UTF_8);
                if (decoded.equals(path)) break;
                path = decoded;
                if (depth == 3 && path.contains("%")) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }
            if (path.contains("%")) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (IllegalArgumentException invalidPath) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String prefix = request.getContextPath() + "/api/groups/";
        if (path.startsWith(prefix)) {
            String remainder = path.substring(prefix.length());
            String first = remainder.split("/", 2)[0];
            if (first.matches("\\d+")) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication instanceof JwtAuthenticationToken jwt) {
                    Long userId = Long.valueOf(jwt.getName());
                    Long groupId;
                    try {
                        groupId = Long.valueOf(first);
                    } catch (NumberFormatException invalidId) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                    if (!members.existsByGroupIdAndUserId(groupId, userId)) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                        return;
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
