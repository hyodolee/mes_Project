package com.mcs.domain.inventory.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class LocTransHisDto {
    private Long locTransId;
    private String plantCd;
    private Long locStockId;
    private String transType;
    private Double transQty;
    private Double beforeQty;
    private Double afterQty;
    private String refType;
    private String refNo;
    private Long refId;
    private String transRmk;
    private String regUserId;
    private LocalDateTime regDtm;
    // Join Fields
    private String plantNm;
    private String transTypeNm;
    private String warehouseNm;
    private String zoneNm;
    private String locationCd;
    private String itemCd;
    private String itemNm;
    private String lotNo;
}

