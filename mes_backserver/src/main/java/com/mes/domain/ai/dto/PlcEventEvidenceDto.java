package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlcEventEvidenceDto {
    private Long eventId;
    private String equipmentCd;
    private String eventType;
    private String eventStatus;
    private String errorCode;
    private String eventMessage;
    private String eventDtm;
    private String processResult;
    private String processMessage;
}
