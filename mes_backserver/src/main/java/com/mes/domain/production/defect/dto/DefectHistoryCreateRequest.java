package com.mes.domain.production.defect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DefectHistoryCreateRequest(
    @NotBlank String plantCd,
    @NotNull Long resultId,
    Long procResultId,
    @NotNull LocalDate defectDt,
    @NotBlank String defectType,
    @NotBlank String defectCd,
    String defectNm,
    @NotNull BigDecimal defectQty,
    String defectCause,
    String defectAction,
    String disposition,
    BigDecimal dispositionQty,
    LocalDate dispositionDt,
    @NotBlank String itemCd,
    String lotNo,
    String workerId,
    String equipmentCd,
    String processCd,
    String defectRmk
) {}
