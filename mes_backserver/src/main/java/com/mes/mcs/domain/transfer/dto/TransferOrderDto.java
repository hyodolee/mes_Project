package com.mes.mcs.domain.transfer.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class TransferOrderDto {
    private Long transferId;
    private String plantCd;
    private String transferNo;
    private String transferStatus;
    private Long fromLocationId;
    private Long toLocationId;
    private String transferReason;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    // Join Fields
    private String plantNm;
    private String transferStatusNm;
    private String fromLocationCd;
    private String toLocationCd;
    private String optimizeRule;
}

