package com.mcs.domain.outbound.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OutboundOrderDto(
    Long outboundId,
    String plantCd,
    String outboundNo,
    String outboundStatus,
    String customerCd,
    String warehouseCd,
    LocalDateTime requestDt,
    LocalDateTime shippedDt,
    String destination,
    Long issuePlanId,
    Long woId,
    String outboundRmk,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join Fields
    String plantNm,
    String warehouseNm,
    String outboundStatusNm
) {}
