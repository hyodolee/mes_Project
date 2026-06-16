package com.mes.mcs.domain.inbound.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class InboundItemDto {
    private Long inboundItemId;
    private Long inboundId;
    private String itemCd;
    private String lotNo;
    private Long locationId;
    private Double expectedQty;
    private Double actualQty;
    private String itemStatus;
    private String itemRmk;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    // Join Fields
    private String itemNm;
    private String itemSpec;
    private String unit;
    private String locationCd;
    private String itemStatusNm;
}

