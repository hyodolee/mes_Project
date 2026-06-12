package com.mcs.domain.inventory.dto;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class StockAdjustRequest {
    private Long locStockId;
    private String adjustType;
    private Double adjustQty;
    private String transRmk;
    private String regUserId;
}

