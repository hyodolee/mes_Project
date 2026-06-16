package com.mes.mcs.domain.outbound.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class OutboundItemDto {
    private Long outboundItemId;
    private Long outboundId;
    private String itemCd;
    private String lotNo;
    private Long locationId;
    private Double requestedQty;
    private Double allocatedQty;
    private Double pickedQty;
    private Double shippedQty;
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

