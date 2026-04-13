package com.mcs.domain.inbound.dto;

import com.mcs.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InboundSearchDto extends PageRequest {
    private String plantCd;
    private String vendorCd;
    private String warehouseCd;
    private String inboundStatus;
    private String inboundNo;
    private String fromDate; // EXPECTED_DT
    private String toDate;
}
