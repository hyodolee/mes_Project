package com.mes.domain.planning.workorder.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkOrderDto(
        Long woId,
        String plantCd,
        String plantNm,
        String woNo,
        Long planId,
        LocalDate woDt,
        String itemCd,
        String itemNm,
        BigDecimal woQty,
        String workcenterCd,
        String equipmentCd,
        String workerId,
        LocalDateTime planStartDtm,
        LocalDateTime planEndDtm,
        LocalDateTime actualStartDtm,
        LocalDateTime actualEndDtm,
        BigDecimal goodQty,
        BigDecimal defectQty,
        String woStatus,
        Integer priority,
        String lotNo,
        Long parentWoId,
        String orderNo,
        LocalDate deliveryDt,
        String woRmk
) {
}
