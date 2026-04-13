package com.mes.domain.equipment.downtime.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DowntimeRequest {
    private Long downtimeId; // For useGeneratedKeys
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
    private Long operId;
    private String actionContent;
    private String actionUserId;
    private LocalDateTime actionDtm;
    private String reporterId;
    private String downtimeRmk;
    private String regUserId;
}
