package com.mes.domain.equipment.downtime.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DowntimeDto(
    Long downtimeId,
    String plantCd,
    String equipmentCd,
    LocalDate downtimeDt,
    String downtimeType,
    String downtimeCd,
    String downtimeReason,
    LocalDateTime startDtm,
    LocalDateTime endDtm,
    BigDecimal downtimeMin,
    Long woId,
    String actionContent,
    LocalDateTime regDtm
) {}
