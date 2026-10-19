package com.zerosum.zerosum.settlement;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/groups/{groupId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(
            SettlementService settlementService
    ) {
        this.settlementService = settlementService;
    }

    @GetMapping("/balances")
    public List<BalanceDTO> getBalances(
            @PathVariable Long groupId
    ) {
        return settlementService.getBalances(groupId);
    }

    @GetMapping("/settlement")
    public SettlementResponseDTO getSettlement(
            @PathVariable Long groupId
    ) {
        return settlementService.getSettlement(groupId);
    }

    @PostMapping("/settlement/whatif")
    public SettlementResponseDTO simulatePayment(
            @PathVariable Long groupId,
            @Valid @RequestBody WhatIfRequest request
    ) {
        return settlementService.simulatePayment(groupId, request);
    }

    @PostMapping("/settlement/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentHistoryItem recordPayment(@PathVariable Long groupId,
            @Valid @RequestBody RecordPaymentRequest request) {
        return settlementService.recordPayment(groupId, request);
    }

    @GetMapping("/settlement/history")
    public List<PaymentHistoryItem> getPaymentHistory(@PathVariable Long groupId) {
        return settlementService.getPaymentHistory(groupId);
    }
}
