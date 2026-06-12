package com.mes.domain.planning.workorder.dto;

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
public class WorkOrderDto {
    private Long woId;
    private String plantCd;
    private String plantNm;
    private String woNo;
    private Long planId;
    private LocalDate woDt;
    private String itemCd;
    private String itemNm;
    private BigDecimal woQty;
    private String workcenterCd;
    private String equipmentCd;
    private String workerId;
    private LocalDateTime planStartDtm;
    private LocalDateTime planEndDtm;
    private LocalDateTime actualStartDtm;
    private LocalDateTime actualEndDtm;
    private BigDecimal goodQty;
    private BigDecimal defectQty;
    private String woStatus;
    private Integer priority;
    private String lotNo;
    private Long parentWoId;
    private String orderNo;
    private LocalDate deliveryDt;
    private String woRmk;
}
