package com.zerosum.zerosum.service;

import com.zerosum.zerosum.dto.CreateExpenseRequest;
import com.zerosum.zerosum.dto.ExpenseSplitInput;
import com.zerosum.zerosum.entity.Group;
import com.zerosum.zerosum.entity.GroupMember;
import com.zerosum.zerosum.entity.User;
import com.zerosum.zerosum.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ExpenseServiceSplitTest {
    private ExpenseService service;
    private User first;
    private User second;

    @BeforeEach
    void setUp() {
        var expenses = mock(ExpenseRepository.class);
        var shares = mock(ExpenseShareRepository.class);
        var groups = mock(GroupRepository.class);
        var members = mock(GroupMemberRepository.class);
        var users = mock(UserRepository.class);
        first = mock(User.class);
        second = mock(User.class);
        when(first.getId()).thenReturn(1L);
        when(first.getName()).thenReturn("A");
        when(second.getId()).thenReturn(2L);
        when(second.getName()).thenReturn("B");
        Group group = mock(Group.class);
        when(group.getId()).thenReturn(7L);
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(users.findById(1L)).thenReturn(Optional.of(first));
        GroupMember secondMember = member(second);
        GroupMember firstMember = member(first);
        when(members.findByGroupIdOrderByUserIdAsc(7L)).thenReturn(List.of(secondMember, firstMember));
        service = new ExpenseService(expenses, shares, groups, members, users, mock(SettlementRepository.class));
    }

    @Test
    void allocatesUnevenEqualSplitDeterministicallyByUserId() {
        var response = preview("equal", 101L, null);
        assertEquals(List.of(51L, 50L), response.shares().stream().map(s -> s.shareAmountPaise()).toList());
    }

    @Test
    void validatesExactPaiseAndSortsSuppliedValues() {
        var response = preview("exact", 101L, List.of(new ExpenseSplitInput(2L, 40L), new ExpenseSplitInput(1L, 61L)));
        assertEquals(List.of(61L, 40L), response.shares().stream().map(s -> s.shareAmountPaise()).toList());
    }

    @Test
    void allocatesPercentageRemainderByStableUserId() {
        var response = preview("percentage", 101L, List.of(new ExpenseSplitInput(2L, 5000L), new ExpenseSplitInput(1L, 5000L)));
        assertEquals(List.of(51L, 50L), response.shares().stream().map(s -> s.shareAmountPaise()).toList());
    }

    @Test
    void allocatesShareRemainderByStableUserId() {
        var response = preview("shares", 101L, List.of(new ExpenseSplitInput(2L, 2L), new ExpenseSplitInput(1L, 1L)));
        assertEquals(List.of(34L, 67L), response.shares().stream().map(s -> s.shareAmountPaise()).toList());
    }

    private com.zerosum.zerosum.dto.ExpenseResponse preview(String type, long amount, List<ExpenseSplitInput> splits) {
        return service.previewExpense(7L, new CreateExpenseRequest("Test", amount, "other", LocalDate.of(2026, 1, 1),
                1L, List.of(1L, 2L), type, splits));
    }

    private GroupMember member(User user) {
        GroupMember member = mock(GroupMember.class);
        when(member.getUser()).thenReturn(user);
        return member;
    }
}
