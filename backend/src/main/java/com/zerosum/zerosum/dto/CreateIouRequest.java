package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateIouRequest(
        @NotNull Long fromUserId,
        @NotNull Long toUserId,
        @NotNull @Positive Long amountPaise,
        String note,
        Long groupId
) {}
