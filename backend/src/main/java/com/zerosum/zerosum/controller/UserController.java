package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    public GroupMemberResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.get(Long.valueOf(jwt.getSubject()));
    }
}
