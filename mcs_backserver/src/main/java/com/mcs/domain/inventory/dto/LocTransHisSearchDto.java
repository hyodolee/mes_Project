package com.mcs.domain.inventory.dto;

import com.mcs.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocTransHisSearchDto extends PageRequest {
    private String plantCd;
    private String fromDate; // YYYY-MM-DD
    private String toDate;   // YYYY-MM-DD
    private String transType;
    private String itemCd;
    private String locationCd;
}
