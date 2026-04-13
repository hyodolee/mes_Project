package com.mcs.domain.location.dto;

import com.mcs.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationSearchDto extends PageRequest {
    private String plantCd;
    private String warehouseCd;
    private Long zoneId;
    private String locationCd;
    private String locationNm;
    private String locationStatus;
}
