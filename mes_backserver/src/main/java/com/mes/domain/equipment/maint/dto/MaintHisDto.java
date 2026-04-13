package com.mes.domain.equipment.maint.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MaintHisDto(
    Long maintId,
    String plantCd,
    String equipmentCd,
    String maintNo,
    String maintType,
    LocalDate maintDt,
    LocalDateTime startDtm,
    LocalDateTime endDtm,
    BigDecimal maintTime,
    String maintWorkerId,
    String maintResult,
    BigDecimal maintCost,
    LocalDateTime regDtm
) {}
