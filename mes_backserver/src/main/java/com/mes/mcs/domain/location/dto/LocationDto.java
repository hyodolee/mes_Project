package com.mes.mcs.domain.location.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class LocationDto {
    private Long locationId;
    private Long zoneId;
    private String locationCd;
    private String locationNm;
    private Double maxCapacity;
    private Double currentUsage;
    private String locationStatus;
    private String useYn;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    private String plantCd;
    private String plantNm;
    private String warehouseCd;
    private String warehouseNm;
    private String zoneCd;
    private String zoneNm;
    private String locationStatusNm;
}
