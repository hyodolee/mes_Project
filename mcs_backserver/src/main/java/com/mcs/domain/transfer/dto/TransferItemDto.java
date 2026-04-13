package com.mcs.domain.transfer.dto;

import java.time.LocalDateTime;

public record TransferItemDto(
    Long transferItemId,
    Long transferId,
    String itemCd,
    String lotNo,
    Double transferQty,
    String itemStatus,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join Fields
    String itemNm,
    String itemSpec,
    String unit,
    String itemStatusNm
) {}
