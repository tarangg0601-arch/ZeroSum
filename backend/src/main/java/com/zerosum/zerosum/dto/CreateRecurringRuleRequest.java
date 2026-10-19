package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record CreateRecurringRuleRequest(
        @NotBlank String description,
        @NotNull @Positive Long amountPaise,
        String category,
        @NotBlank String frequency,
        @NotNull LocalDate nextRunDate,
        @NotNull Long paidBy
) {}
