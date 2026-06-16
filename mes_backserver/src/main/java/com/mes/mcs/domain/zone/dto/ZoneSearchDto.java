package com.mes.mcs.domain.zone.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ZoneSearchDto extends PageRequest {
    private String plantCd;
    private String warehouseCd;
    private String zoneCd;
    private String zoneNm;
}
