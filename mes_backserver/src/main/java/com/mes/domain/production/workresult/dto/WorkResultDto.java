package com.mes.domain.production.workresult.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkResultDto(
    Long resultId,
    String plantCd,
    String resultNo,
    Long woId,
    LocalDate resultDt,
    String shift,
    String workerId,
    String workcenterCd,
    String equipmentCd,
    String itemCd,
    BigDecimal prodQty,
    BigDecimal goodQty,
    BigDecimal defectQty,
    LocalDateTime startDtm,
    LocalDateTime endDtm,
    BigDecimal workTime,
    BigDecimal setupTime,
    BigDecimal downTime,
    String lotNo,
    String resultStatus,
    String resultRmk,
    String regUserId,
    LocalDateTime regDtm
) {}
