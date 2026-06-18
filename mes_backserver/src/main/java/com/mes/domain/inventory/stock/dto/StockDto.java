package com.mes.domain.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockDto {
    private Long stockId;
    private String plantCd;
    private String warehouseCd;
    private String locationCd;
    private String itemCd;
    private String itemNm;
    private String lotNo;
    private BigDecimal stockQty;
    private BigDecimal reservedQty;
    private BigDecimal availableQty;
    private String unit;
    private String stockStatus;
    private BigDecimal safetyStockQty; // 안전재고(기준치) — MST_ITEM 조인
    private LocalDate expireDt;
    private LocalDateTime regDtm;
}
