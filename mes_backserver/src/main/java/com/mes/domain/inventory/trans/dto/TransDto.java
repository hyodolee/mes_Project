package com.mes.domain.inventory.trans.dto;

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
public class TransDto {
    private Long transId;
    private String plantCd;
    private String transNo;
    private LocalDate transDt;
    private String transType;
    private String transReason;
    private String itemCd;
    private String lotNo;
    private BigDecimal transQty;
    private String unit;
    private String fromWarehouseCd;
    private String toWarehouseCd;
    private String refNo;
    private LocalDateTime regDtm;
}
