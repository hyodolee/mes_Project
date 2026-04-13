package com.mes.domain.production.defect.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DefectHistoryDto(
    Long defectId,
    String plantCd,
    Long resultId,
    Long procResultId,
    LocalDate defectDt,
    String defectType,
    String defectCd,
    String defectNm,
    BigDecimal defectQty,
    String defectCause,
    String defectAction,
    String disposition,
    BigDecimal dispositionQty,
    LocalDate dispositionDt,
    String itemCd,
    String lotNo,
    String workerId,
    String equipmentCd,
    String processCd,
    String defectRmk,
    String regUserId,
    LocalDateTime regDtm
) {}
