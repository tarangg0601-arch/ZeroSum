package com.zerosum.zerosum.settlement;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
}
