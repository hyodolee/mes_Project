package com.mcs.domain.plc.dto;

import java.time.LocalDateTime;

public record PlcEventRequest(
        String equipmentCd,
        String eventType,
        String eventStatus,
        String targetType,
        Long targetId,
        String locationCd,
        String errorCode,
        String message,
        LocalDateTime eventDtm
) {
}
