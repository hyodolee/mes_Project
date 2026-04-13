package com.mes.domain.equipment.oper.dto;

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
public class OperStatusRequest {
    private Long operId; // For useGeneratedKeys
    private String plantCd;
    private String equipmentCd;
    private LocalDate operDt;
    private String shift;
    private String operStatus;
    private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal operTime;
    private Long woId;
    private String itemCd;
    private BigDecimal prodQty;
    private String workerId;
    private String operRmk;
    private String regUserId;
}
