package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateGroupRequest(

        @NotBlank(message = "Group name is required")
        String name,

        @NotBlank(message = "Group type is required")
        String groupType

) {
}