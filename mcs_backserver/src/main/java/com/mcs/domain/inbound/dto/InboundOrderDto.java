package com.mcs.domain.inbound.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InboundOrderDto(
    Long inboundId,
    String plantCd,
    String inboundNo,
    String inboundStatus,
    String vendorCd,
    String warehouseCd,
    LocalDate expectedDt,
    LocalDateTime actualDt,
    Long receivePlanId,
    String inboundRmk,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join Fields
    String plantNm,
    String vendorNm,
    String warehouseNm,
    String inboundStatusNm
) {}
