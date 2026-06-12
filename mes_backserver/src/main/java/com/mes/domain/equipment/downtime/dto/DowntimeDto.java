package com.mes.domain.equipment.downtime.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DowntimeDto {
    private Long downtimeId;
    private String plantCd;
    private String equipmentCd;
    private LocalDate downtimeDt;
    private String downtimeType;
    private String downtimeCd;
    private String downtimeReason;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal downtimeMin;
    private Long woId;
    private String actionContent;
    private LocalDateTime regDtm;
}
