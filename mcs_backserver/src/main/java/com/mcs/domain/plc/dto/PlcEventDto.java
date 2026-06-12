package com.mcs.domain.plc.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class PlcEventDto {
    private Long eventId;
    private String equipmentCd;
    private String eventType;
    private String eventStatus;
    private String targetType;
    private Long targetId;
    private String locationCd;
    private String errorCode;
    private String eventMessage;
    private String rawPayload;
    private LocalDateTime eventDtm;
    private String processedYn;
    private String processResult;
    private String processMessage;
    private LocalDateTime processedDtm;
    private String regUserId;
    private LocalDateTime regDtm;
}

