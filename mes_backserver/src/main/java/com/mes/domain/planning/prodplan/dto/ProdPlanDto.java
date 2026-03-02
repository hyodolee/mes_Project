package com.mes.domain.planning.prodplan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProdPlanDto(
        Long planId,
        String plantCd,
        String plantNm,
        String planNo,
        LocalDate planDt,
        String planType,
        String itemCd,
        String itemNm,
        BigDecimal planQty,
        LocalDate planStartDt,
        LocalDate planEndDt,
        Integer priority,
        String orderNo,
        String customerCd,
        String customerNm,
        LocalDate deliveryDt,
        String planStatus,
        BigDecimal resultQty,
        String planRmk
) {
}
