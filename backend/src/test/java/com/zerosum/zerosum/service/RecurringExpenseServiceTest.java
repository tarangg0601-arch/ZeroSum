package com.zerosum.zerosum.service;

import com.zerosum.zerosum.entity.*;
import com.zerosum.zerosum.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringExpenseServiceTest {
    @Mock RecurringRuleRepository rules;
    @Mock ExpenseRepository expenses;
    @Mock ExpenseShareRepository shares;
    @Mock GroupRepository groups;
    @Mock GroupMemberRepository members;
    @Mock UserRepository users;

    @Test
    void generatesDueExpenseOnceAndAdvancesNextDate() {
        LocalDate today = LocalDate.of(2026, 10, 19);
        var service = new RecurringExpenseService(rules, expenses, shares, groups, members, users,
                Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneOffset.UTC));
        var group = new Group();
        var payer = new User();
        var rule = new RecurringRule();
        rule.setGroup(group);
        rule.setPaidBy(payer);
        rule.setDescription("Rent");
        rule.setAmountPaise(101L);
        rule.setFrequency("MONTHLY");
        rule.setNextRunDate(today);
        rule.setActive(true);
        var member = new GroupMember();
        member.setUser(payer);

        when(rules.findByActiveTrueAndNextRunDateLessThanEqualOrderByNextRunDateAsc(today)).thenReturn(List.of(rule));
        when(expenses.existsByRecurringRuleIdAndExpenseDate(null, today)).thenReturn(false);
        when(members.findByGroupId(null)).thenReturn(List.of(member));
        when(expenses.save(any(Expense.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(1, service.generateDueExpenses());
        assertEquals(LocalDate.of(2026, 11, 19), rule.getNextRunDate());
        assertEquals(0, service.generateDueExpenses());
        verify(expenses, times(1)).save(any(Expense.class));
        verify(shares, times(1)).save(any(ExpenseShare.class));
        verify(rules, times(2)).saveAll(any());
    }

    @Test
    void supportsMonthEndAndLeapYearSchedules() {
        assertEquals(LocalDate.of(2026, 2, 28), RecurringExpenseService.advance(LocalDate.of(2026, 1, 31), "monthly"));
        assertEquals(LocalDate.of(2026, 3, 31), RecurringExpenseService.advance(LocalDate.of(2026, 2, 28), "monthly"));
        assertEquals(LocalDate.of(2028, 2, 29), RecurringExpenseService.advance(LocalDate.of(2027, 2, 28), "yearly"));
    }
}
