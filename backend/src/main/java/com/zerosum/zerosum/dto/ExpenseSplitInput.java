package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ExpenseSplitInput(
        @NotNull Long userId,
        @NotNull @PositiveOrZero Long value
) {
}
