package com.zerosum.zerosum.settlement;

public record BalanceDTO(
        Long userId,
        String userName,
        Long balancePaise
) {
}