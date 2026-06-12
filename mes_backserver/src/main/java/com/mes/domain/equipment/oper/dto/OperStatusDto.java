package com.mes.domain.equipment.oper.dto;

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
public class OperStatusDto {
    private Long operId;
    private String plantCd;
    private String equipmentCd;
    private LocalDate operDt;
    private String shift;
    private String operStatus;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal operTime;
    private Long woId;
    private String itemCd;
    private BigDecimal prodQty;
    private String workerId;
    private String operRmk;
    private LocalDateTime regDtm;
}
