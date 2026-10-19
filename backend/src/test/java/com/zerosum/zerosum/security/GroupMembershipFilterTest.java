package com.zerosum.zerosum.security;

import com.zerosum.zerosum.repository.GroupMemberRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class GroupMembershipFilterTest {
    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void blocksAnAuthenticatedUserWhoIsNotInTheRequestedGroup() throws Exception {
        GroupMemberRepository members = mock(GroupMemberRepository.class);
        when(members.existsByGroupIdAndUserId(20L, 7L)).thenReturn(false);
        GroupMembershipFilter filter = new GroupMembershipFilter(members);
        Instant now = Instant.now();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "HS256").subject("7").issuedAt(now).expiresAt(now.plusSeconds(60)).build(), List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/groups/20/settlement");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(404, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void allowsAGroupMemberToContinueToTheEndpoint() throws Exception {
        GroupMemberRepository members = mock(GroupMemberRepository.class);
        when(members.existsByGroupIdAndUserId(20L, 7L)).thenReturn(true);
        GroupMembershipFilter filter = new GroupMembershipFilter(members);
        Instant now = Instant.now();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(Jwt.withTokenValue("token")
                .header("alg", "HS256").subject("7").issuedAt(now).expiresAt(now.plusSeconds(60)).build(), List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/groups/20/settlement");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
