package com.mcs.domain.mes.dto;

public record ItemDto(
    String itemCd,
    String plantCd,
    String itemNm,
    String itemSpec,
    String itemType,
    String itemGrp,
    String unit,
    Double safetyStockQty,
    String mainVendorCd,
    String mainVendorNm
) {}
