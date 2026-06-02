package com.mes.domain.planning.workorder.dto;

public record MaterialTransferResponse(
        Long woId,
        String woNo,
        Long transferId,
        String transferNo,
        Long fromLocationId,
        String fromLocationCd,
        Long toLocationId,
        String toLocationCd,
        String itemCd,
        String lotNo,
        Double transferQty,
        String optimizeRule
) {
}
