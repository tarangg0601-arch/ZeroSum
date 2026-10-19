package com.zerosum.zerosum.settlement;

import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.ExpenseShareResponse;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.dto.CreateExpenseRequest;
import com.zerosum.zerosum.service.ExpenseService;
import com.zerosum.zerosum.service.GroupService;
import com.zerosum.zerosum.repository.GroupRepository;
import com.zerosum.zerosum.repository.SettlementRepository;
import com.zerosum.zerosum.repository.UserRepository;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SettlementServicePreviewTest {
    @Test
    void preventsAnAccountFromRecordingSomeoneElsesPayment() {
        SettlementService service = new SettlementService(new SettlementEngine(), mock(GroupService.class), mock(ExpenseService.class),
                mock(SettlementRepository.class), mock(GroupRepository.class), mock(UserRepository.class));
        assertThrows(IllegalArgumentException.class, () -> service.recordPayment(7L, new RecordPaymentRequest(1L, 2L, 100L), 3L));
    }

    @Test
    void hypotheticalExpenseRecalculatesBalancesWithoutPersisting() {
        SettlementEngine engine = new SettlementEngine();
        GroupService groups = mock(GroupService.class);
        ExpenseService expenses = mock(ExpenseService.class);
        SettlementRepository settlements = mock(SettlementRepository.class);
        SettlementService service = new SettlementService(engine, groups, expenses, settlements, mock(GroupRepository.class), mock(UserRepository.class));
        when(groups.getMembers(7L)).thenReturn(List.of(new GroupMemberResponse(1L, "A", null), new GroupMemberResponse(2L, "B", null)));
        when(expenses.getExpensesByGroup(7L)).thenReturn(List.of());
        when(settlements.findByGroupIdAndStatusOrderBySettledAtDesc(7L, "completed")).thenReturn(List.of());
        when(expenses.previewExpense(eq(7L), any())).thenReturn(new ExpenseResponse(null, 7L, 2L, "B", "Hotel", 1000L, "travel",
                LocalDate.of(2026, 10, 19), false, null, List.of(new ExpenseShareResponse(1L, "A", 500L, "equal", true),
                new ExpenseShareResponse(2L, "B", 500L, "equal", true))));

        var request = new WhatIfRequest(null, null, null, new CreateExpenseRequest("Hotel", 1000L, "travel",
                LocalDate.of(2026, 10, 19), 2L, List.of(1L, 2L), "equal", null));
        var response = service.simulatePayment(7L, request);
        assertEquals(List.of(-500L, 500L), response.balances().stream().map(BalanceDTO::balancePaise).toList());
        assertEquals(1, response.transactions().size());
        verify(expenses).previewExpense(eq(7L), any());
        verify(settlements, never()).save(any());
    }
}
