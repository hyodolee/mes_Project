package com.mes.domain.production.defect.dto;

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
public class DefectHistoryDto {
    private Long defectId;
    private String plantCd;
    private Long resultId;
    private Long procResultId;
    private LocalDate defectDt;
    private String defectType;
    private String defectCd;
    private String defectNm;
    private BigDecimal defectQty;
    private String defectCause;
    private String defectAction;
    private String disposition;
    private BigDecimal dispositionQty;
    private LocalDate dispositionDt;
    private String itemCd;
    private String lotNo;
    private String workerId;
    private String equipmentCd;
    private String processCd;
    private String defectRmk;
    private String regUserId;
    private LocalDateTime regDtm;
}
