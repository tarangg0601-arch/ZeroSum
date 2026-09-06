package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGroupRequest(

        @NotBlank(message = "Group name is required")
        String name,

        @NotNull(message = "Group type is required")
        String groupType,

        @NotNull(message = "Creator ID is required")
        Long createdBy

) {
}