package com.mes.domain.inventory.stock.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StockDto(
    Long stockId,
    String plantCd,
    String warehouseCd,
    String locationCd,
    String itemCd,
    String itemNm,
    String lotNo,
    BigDecimal stockQty,
    BigDecimal reservedQty,
    BigDecimal availableQty,
    String unit,
    String stockStatus,
    LocalDate expireDt,
    LocalDateTime regDtm
) {}
