package com.mes.domain.inventory.trans.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransDto(
    Long transId,
    String plantCd,
    String transNo,
    LocalDate transDt,
    String transType,
    String transReason,
    String itemCd,
    String lotNo,
    BigDecimal transQty,
    String unit,
    String fromWarehouseCd,
    String toWarehouseCd,
    String refNo,
    LocalDateTime regDtm
) {}
