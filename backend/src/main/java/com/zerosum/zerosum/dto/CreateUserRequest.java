package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank String name, @Email String email) {}
