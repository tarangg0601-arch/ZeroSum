package com.zerosum.zerosum.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponse(

        Long id,

        Long groupId,

        Long paidBy,

        String paidByName,

        String description,

        Long amountPaise,

        String category,

        LocalDate expenseDate,

        boolean recurring,

        Instant createdAt,

        List<ExpenseShareResponse> shares

) {
}