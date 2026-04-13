package com.mcs.domain.inventory.dto;

public record StockAdjustRequest(
    Long locStockId,
    String adjustType, // "ADJ_PLUS" or "ADJ_MINUS"
    Double adjustQty,
    String transRmk,
    String regUserId
) {}
