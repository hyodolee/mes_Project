package com.mes.mcs.domain.inbound.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class InboundOrderDto {
    private Long inboundId;
    private String plantCd;
    private String inboundNo;
    private String inboundStatus;
    private String vendorCd;
    private String warehouseCd;
    private LocalDate expectedDt;
    private LocalDateTime actualDt;
    private Long receivePlanId;
    private String inboundRmk;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    // Join Fields
    private String plantNm;
    private String vendorNm;
    private String warehouseNm;
    private String inboundStatusNm;
}

