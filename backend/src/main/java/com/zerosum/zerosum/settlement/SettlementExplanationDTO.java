package com.zerosum.zerosum.settlement;

import java.util.List;

public record SettlementExplanationDTO(
        List<String> steps
) {
}