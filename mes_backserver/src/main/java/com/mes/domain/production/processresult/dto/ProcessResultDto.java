package com.mes.domain.production.processresult.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProcessResultDto(
    Long procResultId,
    Long resultId,
    Long routingId,
    Integer processSeq,
    String processCd,
    String processNm,
    LocalDateTime startDtm,
    LocalDateTime endDtm,
    BigDecimal inputQty,
    BigDecimal outputQty,
    BigDecimal goodQty,
    BigDecimal defectQty,
    String workerId,
    String equipmentCd,
    BigDecimal workTime,
    String processStatus,
    String lotNo,
    String procRmk,
    String regUserId,
    LocalDateTime regDtm
) {}
