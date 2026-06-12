package com.mcs.domain.outbound.dto;

import java.time.LocalDateTime;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class OutboundOrderDto {
    private Long outboundId;
    private String plantCd;
    private String outboundNo;
    private String outboundStatus;
    private String customerCd;
    private String warehouseCd;
    private LocalDateTime requestDt;
    private LocalDateTime shippedDt;
    private String destination;
    private Long issuePlanId;
    private Long woId;
    private String outboundRmk;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    // Join Fields
    private String plantNm;
    private String warehouseNm;
    private String outboundStatusNm;
}

