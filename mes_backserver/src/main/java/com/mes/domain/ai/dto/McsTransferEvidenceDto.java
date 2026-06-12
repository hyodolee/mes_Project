package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class McsTransferEvidenceDto {
    private Long transferId;
    private String transferNo;
    private String transferStatus;
    private String transferStatusNm;
    private String fromLocationCd;
    private String toLocationCd;
    private String optimizeRule;
}
