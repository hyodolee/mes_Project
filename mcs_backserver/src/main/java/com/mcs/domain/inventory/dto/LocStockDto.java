package com.mcs.domain.inventory.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class LocStockDto {
    private Long locStockId;
    private String plantCd;
    private Long locationId;
    private String itemCd;
    private String lotNo;
    private Double stockQty;
    private Double reservedQty;
    private Double availableQty;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    // Join Fields
    private String plantNm;
    private String warehouseNm;
    private String zoneNm;
    private String locationCd;
    private String locationNm;
    private String itemNm;
    private String itemSpec;
    private String unit;
}

