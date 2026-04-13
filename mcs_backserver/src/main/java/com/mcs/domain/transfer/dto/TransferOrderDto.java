package com.mcs.domain.transfer.dto;

import java.time.LocalDateTime;

public record TransferOrderDto(
    Long transferId,
    String plantCd,
    String transferNo,
    String transferStatus,
    Long fromLocationId,
    Long toLocationId,
    String transferReason,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join Fields
    String plantNm,
    String transferStatusNm,
    String fromLocationCd,
    String toLocationCd
) {}
