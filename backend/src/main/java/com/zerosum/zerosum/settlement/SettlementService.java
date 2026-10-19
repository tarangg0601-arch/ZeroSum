package com.zerosum.zerosum.settlement;

import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.service.ExpenseService;
import com.zerosum.zerosum.service.GroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.util.List;

@Service
public class SettlementService {

    private final SettlementEngine settlementEngine;
    private final GroupService groupService;
    private final ExpenseService expenseService;

    public SettlementService(
            SettlementEngine settlementEngine,
            GroupService groupService,
            ExpenseService expenseService
    ) {
        this.settlementEngine = settlementEngine;
        this.groupService = groupService;
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public List<BalanceDTO> getBalances(Long groupId) {

        List<GroupMemberResponse> members =
                groupService.getMembers(groupId);

        List<ExpenseResponse> expenses =
                expenseService.getExpensesByGroup(groupId);

        return settlementEngine.calculateBalances(
                members,
                expenses
        );
    }

    @Transactional(readOnly = true)
    public SettlementResponseDTO getSettlement(Long groupId) {

        List<BalanceDTO> balances =
                getBalances(groupId);

        List<SettlementTransactionDTO> transactions =
                settlementEngine.calculateSettlements(
                        balances
                );

        SettlementExplanationDTO explanation =
                settlementEngine.explainSettlements(
                        transactions
                );

        return new SettlementResponseDTO(
                balances,
                transactions,
                explanation
        );
    }

    @Transactional(readOnly = true)
    public SettlementResponseDTO simulatePayment(Long groupId, WhatIfRequest request) {
        List<BalanceDTO> current = getBalances(groupId);
        if (request.fromUserId().equals(request.toUserId())) {
            throw new IllegalArgumentException("A payment must be between two different members");
        }
        var debt = current.stream().filter(b -> b.userId().equals(request.fromUserId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payer must be a member of the group"));
        var credit = current.stream().filter(b -> b.userId().equals(request.toUserId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recipient must be a member of the group"));
        if (debt.balancePaise() >= 0 || credit.balancePaise() <= 0 || request.amountPaise() > -debt.balancePaise()
                || request.amountPaise() > credit.balancePaise()) {
            throw new IllegalArgumentException("Payment must not exceed the payer's debt or recipient's balance");
        }
        List<BalanceDTO> simulated = new ArrayList<>(current.size());
        for (BalanceDTO balance : current) {
            long value = balance.balancePaise();
            if (balance.userId().equals(request.fromUserId())) value = Math.addExact(value, request.amountPaise());
            if (balance.userId().equals(request.toUserId())) value = Math.subtractExact(value, request.amountPaise());
            simulated.add(new BalanceDTO(balance.userId(), balance.userName(), value));
        }
        List<SettlementTransactionDTO> transactions = settlementEngine.calculateSettlements(simulated);
        return new SettlementResponseDTO(simulated, transactions, settlementEngine.explainSettlements(transactions));
    }
}
