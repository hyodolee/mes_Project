package com.mes.domain.planning.prodplan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdPlanDto {
    private Long planId;
    private String plantCd;
    private String plantNm;
    private String planNo;
    private LocalDate planDt;
    private String planType;
    private String itemCd;
    private String itemNm;
    private BigDecimal planQty;
    private LocalDate planStartDt;
    private LocalDate planEndDt;
    private Integer priority;
    private String orderNo;
    private String customerCd;
    private String customerNm;
    private LocalDate deliveryDt;
    private String planStatus;
    private BigDecimal resultQty;
    private String planRmk;
}
