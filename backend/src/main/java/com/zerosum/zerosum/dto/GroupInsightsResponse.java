package com.zerosum.zerosum.dto;

import java.time.LocalDate;
import java.util.List;

public record GroupInsightsResponse(Long groupId, LocalDate from, LocalDate to, Long totalAmountPaise,
        Long expenseCount, List<CategoryInsight> categories) {}
