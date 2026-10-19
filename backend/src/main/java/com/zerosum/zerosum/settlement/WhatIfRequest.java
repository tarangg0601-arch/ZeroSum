package com.zerosum.zerosum.settlement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WhatIfRequest(
        @NotNull Long fromUserId,
        @NotNull Long toUserId,
        @NotNull @Positive Long amountPaise
) {}
