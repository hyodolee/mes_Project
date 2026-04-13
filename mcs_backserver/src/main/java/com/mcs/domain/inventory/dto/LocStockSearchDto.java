package com.mcs.domain.inventory.dto;

import com.mcs.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocStockSearchDto extends PageRequest {
    private String plantCd;
    private String warehouseCd;
    private Long zoneId;
    private String locationCd;
    private String itemCd;
    private String lotNo;
    private Boolean excludeZeroStock; // 재고 0 제외
}
