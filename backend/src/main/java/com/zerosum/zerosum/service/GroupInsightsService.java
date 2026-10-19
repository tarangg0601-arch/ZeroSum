package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.CategoryInsight;
import com.zerosum.zerosum.dto.GroupInsightsResponse;
import com.zerosum.zerosum.exception.ResourceNotFoundException;
import com.zerosum.zerosum.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

@Service
public class GroupInsightsService {
    private final GroupRepository groups;
    private final ExpenseService expenses;

    public GroupInsightsService(GroupRepository groups, ExpenseService expenses) {
        this.groups = groups;
        this.expenses = expenses;
    }

    @Transactional(readOnly = true)
    public GroupInsightsResponse getInsights(Long groupId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) throw new IllegalArgumentException("Start date must be on or before end date");
        if (!groups.existsById(groupId)) throw new ResourceNotFoundException("Group not found");
        var included = expenses.getExpensesByGroup(groupId).stream()
                .filter(expense -> from == null || !expense.expenseDate().isBefore(from))
                .filter(expense -> to == null || !expense.expenseDate().isAfter(to)).toList();
        Map<String, long[]> totals = new HashMap<>();
        long total = 0;
        for (var expense : included) {
            total = Math.addExact(total, expense.amountPaise());
            String category = expense.category() == null || expense.category().isBlank() ? "Uncategorized" : expense.category();
            long[] aggregate = totals.computeIfAbsent(category, ignored -> new long[2]);
            aggregate[0] = Math.addExact(aggregate[0], expense.amountPaise());
            aggregate[1]++;
        }
        var categories = totals.entrySet().stream()
                .map(entry -> new CategoryInsight(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .sorted(Comparator.comparing(CategoryInsight::amountPaise).reversed())
                .toList();
        return new GroupInsightsResponse(groupId, from, to, total, (long) included.size(), categories);
    }
}
