package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(

        @NotBlank(message = "Group name is required")
        @Size(max = 150, message = "Group name must be 150 characters or fewer")
        String name,

        @NotBlank(message = "Group type is required")
        @Pattern(regexp = "general|hostel|trip", message = "Group type must be general, hostel, or trip")
        String groupType

) {
}
