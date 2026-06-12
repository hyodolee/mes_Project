package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderEvidenceDto {
    private Long woId;
    private String woNo;
    private String plantNm;
    private String itemCd;
    private String itemNm;
    private String woStatus;
    private String lotNo;
}
