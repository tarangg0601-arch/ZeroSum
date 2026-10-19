package com.zerosum.zerosum.controller;

import com.zerosum.zerosum.dto.CreateUserRequest;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
}
