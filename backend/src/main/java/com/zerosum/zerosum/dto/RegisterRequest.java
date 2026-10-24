package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,32}") String username,
                              @Size(max = 100) String name,
                              @NotBlank @Size(min = 12, max = 128) String password) { }
