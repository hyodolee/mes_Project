package com.mes.domain.quality.inspectstd.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InspectStdDto(
    Long inspectStdId,
    String plantCd,
    String itemCd,
    String inspectType,
    Integer inspectItemSeq,
    String inspectItem,
    String inspectMethod,
    String dataType,
    String specValue,
    BigDecimal lsl,
    BigDecimal usl,
    BigDecimal targetValue,
    String unit,
    Integer sampleSize,
    String sampleMethod,
    String inspectTool,
    String mandatoryYn,
    String processCd,
    LocalDate validStartDt,
    LocalDate validEndDt,
    String stdRmk,
    String useYn,
    String regUserId,
    LocalDateTime regDtm
) {}
