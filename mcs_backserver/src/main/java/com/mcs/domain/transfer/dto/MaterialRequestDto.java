package com.mcs.domain.transfer.dto;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MaterialRequestDto {
    private String sourceSystem;
    private Long woId;
    private String woNo;
    private String plantCd;
    private String itemCd;
    private Double requestQty;
    private String workcenterCd;
    private String optimizeRule;
    private String requestReason;
}

