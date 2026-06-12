package com.mcs.domain.zone.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ZoneDto {
    private Long zoneId;
    private String plantCd;
    private String warehouseCd;
    private String zoneCd;
    private String zoneNm;
    private String zoneType;
    private Integer sortSeq;
    private String useYn;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    // Join 필드
    private String plantNm;
    private String warehouseNm;
    private String zoneTypeNm;
}

