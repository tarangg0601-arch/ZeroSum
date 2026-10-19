package com.zerosum.zerosum.settlement;

import java.time.Instant;

public record PaymentHistoryItem(Long id, Long fromUserId, String fromUserName, Long toUserId,
        String toUserName, Long amountPaise, String status, Instant settledAt) {}
