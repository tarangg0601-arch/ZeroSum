package com.zerosum.zerosum.settlement;

import jakarta.validation.Valid;
import com.zerosum.zerosum.dto.CreateExpenseRequest;

public record WhatIfRequest(
        Long fromUserId,
        Long toUserId,
        Long amountPaise,
        @Valid CreateExpenseRequest expense
) {}
