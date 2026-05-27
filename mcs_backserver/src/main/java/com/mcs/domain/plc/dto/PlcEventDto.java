package com.mcs.domain.plc.dto;

import java.time.LocalDateTime;

public record PlcEventDto(
        Long eventId,
        String equipmentCd,
        String eventType,
        String eventStatus,
        String targetType,
        Long targetId,
        String locationCd,
        String errorCode,
        String eventMessage,
        String rawPayload,
        LocalDateTime eventDtm,
        String processedYn,
        String processResult,
        String processMessage,
        LocalDateTime processedDtm,
        String regUserId,
        LocalDateTime regDtm
) {
}
