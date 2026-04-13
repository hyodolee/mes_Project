package com.mes.domain.inventory.stock.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockSearchDto extends PageRequest {
    private String plantCd;
    private String warehouseCd;
    private String itemCd;
    private String lotNo;
    private String stockStatus;
}
