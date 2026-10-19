package com.zerosum.zerosum.repository;

import com.zerosum.zerosum.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDate;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByExpenseDateDescCreatedAtDesc(Long groupId);

    boolean existsByRecurringRuleIdAndExpenseDate(Long recurringRuleId, LocalDate expenseDate);
}
