package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.repository.GroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupInsightsServiceTest {
    @Mock GroupRepository groups;
    @Mock ExpenseService expenses;

    @Test
    void summarizesRecordedExpensesAndAppliesInclusiveDateFilters() {
        var service = new GroupInsightsService(groups, expenses);
        when(groups.existsById(7L)).thenReturn(true);
        when(expenses.getExpensesByGroup(7L)).thenReturn(List.of(
                expense(1000L, "Food", LocalDate.of(2026, 10, 1)),
                expense(500L, "Food", LocalDate.of(2026, 10, 31)),
                expense(900L, "Rent", LocalDate.of(2026, 11, 1))));

        var result = service.getInsights(7L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(1500L, result.totalAmountPaise());
        assertEquals(2L, result.expenseCount());
        assertEquals("Food", result.categories().get(0).category());
        assertEquals(1500L, result.categories().get(0).amountPaise());
    }

    private ExpenseResponse expense(long amount, String category, LocalDate date) {
        return new ExpenseResponse(1L, 7L, 1L, "A", "Expense", amount, category, date, false,
                Instant.parse("2026-10-19T00:00:00Z"), List.of());
    }
}
