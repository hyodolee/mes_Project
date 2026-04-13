package com.mcs.domain.zone.dto;

import java.time.LocalDateTime;

public record ZoneDto(
    Long zoneId,
    String plantCd,
    String warehouseCd,
    String zoneCd,
    String zoneNm,
    String zoneType,
    Integer sortSeq,
    String useYn,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join 필드
    String plantNm,
    String warehouseNm,
    String zoneTypeNm
) {}
