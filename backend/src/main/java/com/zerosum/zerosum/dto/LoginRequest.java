package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,32}") String username,
                           @NotBlank String password) { }
