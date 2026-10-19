package com.zerosum.zerosum.dto;

import java.time.Instant;

public record IouResponse(Long id, Long groupId, Long fromUserId, String fromUserName,
        Long toUserId, String toUserName, Long amountPaise, String note, boolean settled, Instant createdAt) {}
