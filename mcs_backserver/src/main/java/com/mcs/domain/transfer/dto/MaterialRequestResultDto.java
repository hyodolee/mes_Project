package com.mcs.domain.transfer.dto;

public record MaterialRequestResultDto(
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
