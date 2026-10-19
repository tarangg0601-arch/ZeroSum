package com.zerosum.zerosum.settlement;

import com.zerosum.zerosum.dto.ExpenseResponse;
import com.zerosum.zerosum.dto.GroupMemberResponse;
import com.zerosum.zerosum.service.ExpenseService;
import com.zerosum.zerosum.service.GroupService;
import com.zerosum.zerosum.entity.Settlement;
import com.zerosum.zerosum.repository.SettlementRepository;
import com.zerosum.zerosum.repository.GroupRepository;
import com.zerosum.zerosum.repository.UserRepository;
import com.zerosum.zerosum.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.util.List;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private final SettlementEngine settlementEngine;
    private final GroupService groupService;
    private final ExpenseService expenseService;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public SettlementService(
            SettlementEngine settlementEngine,
            GroupService groupService,
            ExpenseService expenseService,
            SettlementRepository settlementRepository,
            GroupRepository groupRepository,
            UserRepository userRepository
    ) {
        this.settlementEngine = settlementEngine;
        this.groupService = groupService;
        this.expenseService = expenseService;
        this.settlementRepository = settlementRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<BalanceDTO> getBalances(Long groupId) {

        List<GroupMemberResponse> members =
                groupService.getMembers(groupId);

        List<ExpenseResponse> expenses =
                expenseService.getExpensesByGroup(groupId);

        List<BalanceDTO> calculated = settlementEngine.calculateBalances(
                members,
                expenses
        );
        Map<Long, Long> balances = calculated.stream().collect(Collectors.toMap(BalanceDTO::userId, BalanceDTO::balancePaise));
        for (Settlement payment : settlementRepository.findByGroupIdAndStatusOrderBySettledAtDesc(groupId, "completed")) {
            balances.compute(payment.getFromUser().getId(), (id, value) -> Math.addExact(value, payment.getAmountPaise()));
            balances.compute(payment.getToUser().getId(), (id, value) -> Math.subtractExact(value, payment.getAmountPaise()));
        }
        return calculated.stream().map(balance -> new BalanceDTO(balance.userId(), balance.userName(), balances.get(balance.userId()))).toList();
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
        if (request.expense() != null) {
            ExpenseResponse hypothetical = expenseService.previewExpense(groupId, request.expense());
            Map<Long, Long> values = current.stream().collect(Collectors.toMap(BalanceDTO::userId, BalanceDTO::balancePaise));
            values.compute(hypothetical.paidBy(), (id, balance) -> Math.addExact(balance, hypothetical.amountPaise()));
            hypothetical.shares().forEach(share -> values.compute(share.userId(), (id, balance) -> Math.subtractExact(balance, share.shareAmountPaise())));
            List<BalanceDTO> projected = current.stream().map(balance -> new BalanceDTO(balance.userId(), balance.userName(), values.get(balance.userId()))).toList();
            List<SettlementTransactionDTO> plan = settlementEngine.calculateSettlements(projected);
            return new SettlementResponseDTO(projected, plan, settlementEngine.explainSettlements(plan));
        }
        if (request.fromUserId() == null || request.toUserId() == null || request.amountPaise() == null || request.amountPaise() <= 0) {
            throw new IllegalArgumentException("Provide a payment or hypothetical expense");
        }
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

    @Transactional
    public PaymentHistoryItem recordPayment(Long groupId, RecordPaymentRequest request, Long actorId) {
        if (!actorId.equals(request.fromUserId()) && !actorId.equals(request.toUserId())) {
            throw new IllegalArgumentException("You must be a participant in the recorded payment");
        }
        if (request.fromUserId().equals(request.toUserId())) throw new IllegalArgumentException("A payment must be between two different members");
        List<BalanceDTO> balances = getBalances(groupId);
        BalanceDTO payerBalance = balances.stream().filter(b -> b.userId().equals(request.fromUserId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Payer must be a member of the group"));
        BalanceDTO recipientBalance = balances.stream().filter(b -> b.userId().equals(request.toUserId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Recipient must be a member of the group"));
        if (payerBalance.balancePaise() >= 0 || recipientBalance.balancePaise() <= 0
                || request.amountPaise() > -payerBalance.balancePaise() || request.amountPaise() > recipientBalance.balancePaise()) {
            throw new IllegalArgumentException("Payment must not exceed the payer's debt or recipient's balance");
        }
        Settlement payment = new Settlement();
        payment.setGroup(groupRepository.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Group not found")));
        payment.setFromUser(userRepository.findById(request.fromUserId()).orElseThrow(() -> new ResourceNotFoundException("Payer not found")));
        payment.setToUser(userRepository.findById(request.toUserId()).orElseThrow(() -> new ResourceNotFoundException("Recipient not found")));
        payment.setAmountPaise(request.amountPaise());
        payment.setStatus("completed");
        payment.setSettledAt(Instant.now());
        return toHistoryItem(settlementRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<PaymentHistoryItem> getPaymentHistory(Long groupId) {
        groupService.getMembers(groupId);
        return settlementRepository.findByGroupIdOrderBySettledAtDesc(groupId).stream().map(this::toHistoryItem).toList();
    }

    private PaymentHistoryItem toHistoryItem(Settlement payment) {
        return new PaymentHistoryItem(payment.getId(), payment.getFromUser().getId(), payment.getFromUser().getName(),
                payment.getToUser().getId(), payment.getToUser().getName(), payment.getAmountPaise(), payment.getStatus(), payment.getSettledAt());
    }
}
