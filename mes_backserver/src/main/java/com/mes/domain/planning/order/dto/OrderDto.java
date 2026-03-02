package com.mes.domain.planning.order.dto;

import java.time.LocalDate;

public record OrderDto(
        String orderNo,
        String customerCd,
        String customerNm,
        String itemCd,
        String itemNm,
        Integer orderQty,
        LocalDate orderDt,
        LocalDate deliveryDt
) {
}
