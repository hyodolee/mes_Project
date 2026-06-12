package com.mes.domain.planning.workorder.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialTransferRequest {

    private String itemCd;

    @Positive(message = "이동 수량은 0보다 커야 합니다.")
    private BigDecimal transferQty;

    private String optimizeRule;

    private String requestReason;
}
