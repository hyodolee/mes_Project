package com.mcs.domain.inventory.dto;

import java.time.LocalDateTime;

public record LocTransHisDto(
    Long locTransId,
    String plantCd,
    Long locStockId,
    String transType,
    Double transQty,
    Double beforeQty,
    Double afterQty,
    String refType,
    String refNo,
    Long refId,
    String transRmk,
    String regUserId,
    LocalDateTime regDtm,
    // Join Fields
    String plantNm,
    String transTypeNm,
    String warehouseNm,
    String zoneNm,
    String locationCd,
    String itemCd,
    String itemNm,
    String lotNo
) {}
