package com.mes.domain.master.item.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {
    private String itemCd;
    private String plantCd;
    private String itemNm;
    private String itemSpec;
    private String itemType;
    private String itemGrp;
    private String unit;
    private String useYn;
}
