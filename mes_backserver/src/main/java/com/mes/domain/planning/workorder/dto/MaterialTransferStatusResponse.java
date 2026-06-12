package com.mes.domain.planning.workorder.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MaterialTransferStatusResponse {
    private Long woId;
    private boolean requested;
    private boolean completed;
    private boolean startable;
    private Long transferId;
    private String transferNo;
    private String transferStatus;
    private String transferStatusNm;
    private String fromLocationCd;
    private String toLocationCd;
    private String message;
}
