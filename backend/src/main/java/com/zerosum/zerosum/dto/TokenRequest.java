package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank String token) { }
