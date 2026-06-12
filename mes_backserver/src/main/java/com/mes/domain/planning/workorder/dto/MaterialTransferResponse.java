package com.mes.domain.planning.workorder.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialTransferResponse {
    private Long woId;
    private String woNo;
    private Long transferId;
    private String transferNo;
    private Long fromLocationId;
    private String fromLocationCd;
    private Long toLocationId;
    private String toLocationCd;
    private String itemCd;
    private String lotNo;
    private Double transferQty;
    private String optimizeRule;
}
