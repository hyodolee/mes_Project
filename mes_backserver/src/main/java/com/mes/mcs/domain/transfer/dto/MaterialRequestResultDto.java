package com.mes.mcs.domain.transfer.dto;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class MaterialRequestResultDto {
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

