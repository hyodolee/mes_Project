package com.mes.domain.equipment.oper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record OperStatusDto(
    Long operId,
    String plantCd,
    String equipmentCd,
    LocalDate operDt,
    String shift,
    String operStatus,
    LocalDateTime startDtm,
    LocalDateTime endDtm,
    BigDecimal operTime,
    Long woId,
    String itemCd,
    BigDecimal prodQty,
    String workerId,
    String operRmk,
    LocalDateTime regDtm
) {}
