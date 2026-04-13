package com.mes.domain.inventory.trans.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransRequest {
    private String plantCd;
    private String transType; // 입고, 출고, 이동, 조정
    private String transReason;
    private String itemCd;
    private String lotNo;
    private BigDecimal transQty;
    private String unit;
    private String fromWarehouseCd;
    private String toWarehouseCd;
    private String fromLocationCd;
    private String toLocationCd;
    private String refType;
    private String refNo;
    private Long refId;
    private String vendorCd;
    private String transUserId;
    private String transRmk;
    private String regUserId;
}
