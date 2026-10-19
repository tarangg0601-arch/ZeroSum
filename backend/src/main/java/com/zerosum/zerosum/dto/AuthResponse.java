package com.zerosum.zerosum.dto;

public record AuthResponse(Long userId, String name, String email, String token) { }
