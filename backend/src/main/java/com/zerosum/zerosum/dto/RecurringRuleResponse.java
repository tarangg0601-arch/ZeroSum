package com.zerosum.zerosum.dto;

import java.time.LocalDate;

public record RecurringRuleResponse(
        Long id,
        Long groupId,
        Long paidBy,
        String description,
        Long amountPaise,
        String category,
        String frequency,
        LocalDate nextRunDate,
        String anchorType,
        boolean active
) {}
