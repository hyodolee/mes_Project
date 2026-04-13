package com.mcs.domain.location.dto;

import java.time.LocalDateTime;

public record LocationDto(
    Long locationId,
    Long zoneId,
    String locationCd,
    String locationNm,
    Double maxCapacity,
    Double currentUsage,
    String locationStatus,
    String useYn,
    String regUserId,
    LocalDateTime regDtm,
    String updUserId,
    LocalDateTime updDtm,
    // Join 필드
    String plantCd,
    String plantNm,
    String warehouseCd,
    String warehouseNm,
    String zoneCd,
    String zoneNm,
    String locationStatusNm
) {}
