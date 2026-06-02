package com.mcs.domain.transfer.dto;

public record MaterialRequestDto(
        String sourceSystem,
        Long woId,
        String woNo,
        String plantCd,
        String itemCd,
        Double requestQty,
        String workcenterCd,
        String optimizeRule,
        String requestReason
) {
}
