package com.zerosum.zerosum.settlement;

public record SettlementTransactionDTO(
        Long fromUserId,
        String fromUserName,
        Long toUserId,
        String toUserName,
        Long amountPaise
) {
}