package com.mes.domain.planning.order.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private String orderNo;
    private String customerCd;
    private String customerNm;
    private String itemCd;
    private String itemNm;
    private Integer orderQty;
    private LocalDate orderDt;
    private LocalDate deliveryDt;
}
