package com.zerosum.zerosum.dto;

public record ExpenseShareResponse(

        Long userId,

        String userName,

        Long shareAmountPaise,

        String shareType,

        boolean participated

) {
}