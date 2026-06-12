package com.mes.domain.production.processresult.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResultDto {
    private Long procResultId;
    private Long resultId;
    private Long routingId;
    private Integer processSeq;
    private String processCd;
    private String processNm;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal inputQty;
    private BigDecimal outputQty;
    private BigDecimal goodQty;
    private BigDecimal defectQty;
    private String workerId;
    private String equipmentCd;
    private BigDecimal workTime;
    private String processStatus;
    private String lotNo;
    private String procRmk;
    private String regUserId;
    private LocalDateTime regDtm;
}
