package com.zerosum.zerosum.settlement;

import java.util.List;

public record SettlementResponseDTO(
        List<BalanceDTO> balances,
        List<SettlementTransactionDTO> transactions,
        SettlementExplanationDTO explanation
) {
}