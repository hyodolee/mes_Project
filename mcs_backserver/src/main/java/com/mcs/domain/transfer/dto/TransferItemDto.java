package com.mcs.domain.transfer.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class TransferItemDto {
    private Long transferItemId;
    private Long transferId;
    private String itemCd;
    private String lotNo;
    private Double transferQty;
    private String itemStatus;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    // Join Fields
    private String itemNm;
    private String itemSpec;
    private String unit;
    private String itemStatusNm;
}

