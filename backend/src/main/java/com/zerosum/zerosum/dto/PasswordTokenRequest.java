package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordTokenRequest(@NotBlank String token,
                                   @NotBlank @Size(min = 12, max = 128) String password) { }
