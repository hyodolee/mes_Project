package com.mcs.domain.outbound.dto;

import com.mcs.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboundSearchDto extends PageRequest {
    private String plantCd;
    private String warehouseCd;
    private String outboundStatus;
    private String outboundNo;
    private String fromDate; // REQUEST_DT (YYYY-MM-DD)
    private String toDate;   // REQUEST_DT (YYYY-MM-DD)
}
