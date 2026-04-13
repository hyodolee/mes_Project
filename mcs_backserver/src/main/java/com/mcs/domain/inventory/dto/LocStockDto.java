package com.mcs.domain.inventory.dto;

import java.time.LocalDateTime;

public record LocStockDto(
    Long locStockId,
    String plantCd,
    Long locationId,
    String itemCd,
    String lotNo,
    Double stockQty,
    Double reservedQty,
    Double availableQty,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join Fields
    String plantNm,
    String warehouseNm,
    String zoneNm,
    String locationCd,
    String locationNm,
    String itemNm,
    String itemSpec,
    String unit
) {}
