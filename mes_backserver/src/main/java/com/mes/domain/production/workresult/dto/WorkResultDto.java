package com.mes.domain.production.workresult.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkResultDto {
    private Long resultId;
    private String plantCd;
    private String resultNo;
    private Long woId;
    private LocalDate resultDt;
    private String shift;
    private String workerId;
    private String workcenterCd;
    private String equipmentCd;
    private String itemCd;
    private BigDecimal prodQty;
    private BigDecimal goodQty;
    private BigDecimal defectQty;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal workTime;
    private BigDecimal setupTime;
    private BigDecimal downTime;
    private String lotNo;
    private String resultStatus;
    private String resultRmk;
    private String regUserId;
    private LocalDateTime regDtm;
}
