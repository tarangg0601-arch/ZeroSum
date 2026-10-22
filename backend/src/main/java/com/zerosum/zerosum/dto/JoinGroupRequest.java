package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinGroupRequest(
        @NotBlank(message = "Join code is required")
        @Size(max = 20, message = "Join code is invalid")
        String joinCode
) {}
