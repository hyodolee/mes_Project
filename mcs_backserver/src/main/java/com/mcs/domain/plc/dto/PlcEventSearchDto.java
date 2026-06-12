package com.mcs.domain.plc.dto;

import com.mcs.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlcEventSearchDto extends PageRequest {
    private Long eventId;
    private String equipmentCd;
    private String eventType;
    private String eventStatus;
    private String processResult;
    private String targetType;
    private Long targetId;
    private String fromDate;
    private String toDate;
}
