package com.mes.domain.inventory.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRequest {
    private String plantCd;
    private String warehouseCd;
    private String locationCd;
    private String itemCd;
    private String lotNo;
    private BigDecimal qty;
    private String unit;
    private String stockStatus;
    private LocalDate expireDt;
    private LocalDate mfgDt;
    private String regUserId;
}
