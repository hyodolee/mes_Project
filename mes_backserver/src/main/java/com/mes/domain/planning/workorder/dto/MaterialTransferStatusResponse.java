package com.mes.domain.planning.workorder.dto;

public record MaterialTransferStatusResponse(
        Long woId,
        boolean requested,
        boolean completed,
        boolean startable,
        Long transferId,
        String transferNo,
        String transferStatus,
        String transferStatusNm,
        String fromLocationCd,
        String toLocationCd,
        String message
) {
}
