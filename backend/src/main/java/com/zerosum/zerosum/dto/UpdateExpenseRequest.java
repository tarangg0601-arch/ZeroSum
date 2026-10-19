package com.zerosum.zerosum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

public record UpdateExpenseRequest(

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        Long amountPaise,

        String category,

        @NotNull(message = "Expense date is required")
        LocalDate expenseDate,

        @NotNull(message = "Payer ID is required")
        Long paidBy,

        @NotNull(message = "Participant IDs are required")
        List<Long> participantIds,

        String splitType,

        List<@Valid ExpenseSplitInput> splits

) {
}
