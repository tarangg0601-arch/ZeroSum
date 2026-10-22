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
        rule.setAnchorDay(today.getDayOfMonth());
        rule.setAnchorMonth(today.getMonthValue());
        rule.setAnchorMonthEnd(false);
        rule.setActive(true);
        var member = new GroupMember();
        member.setUser(payer);

        when(rules.lockDueRules(today)).thenReturn(List.of(rule), List.of());
        when(expenses.existsByRecurringRuleIdAndExpenseDate(null, today)).thenReturn(false);
        when(members.findByGroupIdOrderByUserIdAsc(null)).thenReturn(List.of(member));
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
        LocalDate jan30 = LocalDate.of(2026, 1, 30);
        LocalDate feb28For30 = RecurringExpenseService.advance(jan30, "monthly", 30, 1, false);
        assertEquals(LocalDate.of(2026, 2, 28), feb28For30);
        assertEquals(LocalDate.of(2026, 3, 30), RecurringExpenseService.advance(feb28For30, "monthly", 30, 1, false));

        LocalDate jan31 = LocalDate.of(2026, 1, 31);
        LocalDate febEnd = RecurringExpenseService.advance(jan31, "monthly", 31, 1, true);
        assertEquals(LocalDate.of(2026, 2, 28), febEnd);
        assertEquals(LocalDate.of(2026, 3, 31), RecurringExpenseService.advance(febEnd, "monthly", 31, 1, true));
        assertEquals(LocalDate.of(2028, 2, 29), RecurringExpenseService.advance(
                LocalDate.of(2027, 2, 28), "yearly", 29, 2, true));
        assertEquals(LocalDate.of(2028, 2, 29), RecurringExpenseService.advance(
                LocalDate.of(2027, 2, 28), "yearly", 29, 2, false));
        assertEquals(LocalDate.of(2029, 2, 28), RecurringExpenseService.advance(
                LocalDate.of(2028, 2, 29), "yearly", 29, 2, false));
    }

    @Test
    void catchesUpMissedMonthlyDatesInSingleRun() {
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
        rule.setNextRunDate(LocalDate.of(2026, 8, 19));
        rule.setAnchorDay(19);
        rule.setAnchorMonth(8);
        rule.setAnchorMonthEnd(false);
        rule.setActive(true);
        var member = new GroupMember();
        member.setUser(payer);
        when(rules.lockDueRules(today)).thenReturn(List.of(rule));
        when(members.findByGroupIdOrderByUserIdAsc(null)).thenReturn(List.of(member));
        when(expenses.save(any(Expense.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals(3, service.generateDueExpenses());
        assertEquals(LocalDate.of(2026, 11, 19), rule.getNextRunDate());
        verify(expenses, times(3)).save(any(Expense.class));
        verify(shares, times(3)).save(any(ExpenseShare.class));
    }

    @Test
    void skipsAnOccurrenceAlreadyCreatedBeforeASchedulerRetry() {
        LocalDate today = LocalDate.of(2026, 10, 19);
        var service = new RecurringExpenseService(rules, expenses, shares, groups, members, users,
                Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"), ZoneOffset.UTC));
        var rule = new RecurringRule();
        rule.setNextRunDate(today);
        rule.setFrequency("MONTHLY");
        rule.setAnchorDay(19);
        rule.setAnchorMonth(10);
        rule.setAnchorMonthEnd(false);
        rule.setActive(true);
        when(rules.lockDueRules(today)).thenReturn(List.of(rule), List.of(rule));
        when(expenses.existsByRecurringRuleIdAndExpenseDate(null, today)).thenReturn(true);

        assertEquals(0, service.generateDueExpenses());
        assertEquals(LocalDate.of(2026, 11, 19), rule.getNextRunDate());
        assertEquals(0, service.generateDueExpenses());
        verify(expenses, never()).save(any(Expense.class));
    }
}
