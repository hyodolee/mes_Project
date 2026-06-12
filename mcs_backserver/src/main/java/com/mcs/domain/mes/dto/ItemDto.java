package com.mcs.domain.mes.dto;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ItemDto {
    private String itemCd;
    private String plantCd;
    private String itemNm;
    private String itemSpec;
    private String itemType;
    private String itemGrp;
    private String unit;
    private Double safetyStockQty;
    private String mainVendorCd;
    private String mainVendorNm;
}

