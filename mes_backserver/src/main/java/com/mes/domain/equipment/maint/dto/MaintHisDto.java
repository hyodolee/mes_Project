package com.mes.domain.equipment.maint.dto;

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
public class MaintHisDto {
    private Long maintId;
    private String plantCd;
    private String equipmentCd;
    private String maintNo;
    private String maintType;
    private LocalDate maintDt;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal maintTime;
    private String maintWorkerId;
    private String maintResult;
    private BigDecimal maintCost;
    private LocalDateTime regDtm;
}
