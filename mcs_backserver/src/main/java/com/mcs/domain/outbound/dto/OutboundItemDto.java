package com.mcs.domain.outbound.dto;

import java.time.LocalDateTime;

public record OutboundItemDto(
    Long outboundItemId,
    Long outboundId,
    String itemCd,
    String lotNo,
    Long locationId,
    Double requestedQty,
    Double allocatedQty,
    Double pickedQty,
    Double shippedQty,
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
