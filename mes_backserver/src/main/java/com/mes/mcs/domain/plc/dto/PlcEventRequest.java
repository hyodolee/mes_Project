package com.mes.mcs.domain.plc.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class PlcEventRequest {
    private String equipmentCd;
    private String eventType;
    private String eventStatus;
    private String targetType;
    private Long targetId;
    private String locationCd;
    private String fromLocationCd;
    private String toLocationCd;
    private String lotNo;
    private String errorCode;
    private String message;
    private LocalDateTime eventDtm;
}

