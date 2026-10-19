package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.Email;

public record AddGroupMemberRequest(

        Long userId,
        @Email String email

) {
}
