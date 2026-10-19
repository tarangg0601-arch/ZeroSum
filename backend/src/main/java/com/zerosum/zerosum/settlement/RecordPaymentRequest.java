package com.zerosum.zerosum.settlement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecordPaymentRequest(@NotNull Long fromUserId, @NotNull Long toUserId,
        @NotNull @Positive Long amountPaise) {}
