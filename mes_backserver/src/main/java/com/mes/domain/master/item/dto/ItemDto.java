package com.mes.domain.master.item.dto;

public record ItemDto(
        String itemCd,
        String plantCd,
        String itemNm,
        String itemSpec,
        String itemType,
        String itemGrp,
        String unit,
        String useYn
) {
}
