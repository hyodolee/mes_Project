package com.mcs.domain.inbound.dto;

import java.time.LocalDateTime;

public record InboundItemDto(
    Long inboundItemId,
    Long inboundId,
    String itemCd,
    String lotNo,
    Long locationId,
    Double expectedQty,
    Double actualQty,
    String itemStatus,
    String itemRmk,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join Fields
    String itemNm,
    String itemSpec,
    String unit,
    String locationCd,
    String itemStatusNm
) {}
