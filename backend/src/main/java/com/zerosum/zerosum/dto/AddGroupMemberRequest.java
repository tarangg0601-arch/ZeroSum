package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotNull;

public record AddGroupMemberRequest(

        @NotNull(message = "User ID is required")
        Long userId

) {
}