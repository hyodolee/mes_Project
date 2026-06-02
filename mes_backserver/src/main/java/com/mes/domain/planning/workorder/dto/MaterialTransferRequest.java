package com.mes.domain.planning.workorder.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MaterialTransferRequest(
        String itemCd,
        @Positive(message = "이동 수량은 0보다 커야 합니다.")
        BigDecimal transferQty,
        String optimizeRule,
        String requestReason
) {
}
