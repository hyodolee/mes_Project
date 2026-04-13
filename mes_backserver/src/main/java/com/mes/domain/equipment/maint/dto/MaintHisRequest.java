package com.mes.domain.equipment.maint.dto;

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
public class MaintHisRequest {
    private Long maintId; // For useGeneratedKeys
    private String plantCd;
    private String equipmentCd;
    private String maintType;
    private LocalDate maintDt;
    private Long maintPlanId;
    private Long downtimeId;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal maintTime;
    private String maintWorkerId;
    private String maintTeam;
    private String maintContent;
    private String symptom;
    private String cause;
    private String action;
    private String partReplaced;
    private BigDecimal partCost;
    private BigDecimal laborCost;
    private BigDecimal otherCost;
    private String maintResult;
    private LocalDate nextMaintDt;
    private String maintRmk;
    private String regUserId;
}
