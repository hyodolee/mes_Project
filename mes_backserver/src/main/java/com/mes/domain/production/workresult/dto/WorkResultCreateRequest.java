package com.mes.domain.production.workresult.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class WorkResultCreateRequest {
    private Long resultId; // For useGeneratedKeys

    @NotBlank private String plantCd;
    @NotNull private Long woId;
    @NotNull private LocalDate resultDt;
    @NotBlank private String shift;
    @NotBlank private String workerId;
    @NotBlank private String workcenterCd;
    private String equipmentCd;
    @NotBlank private String itemCd;
    @NotNull private BigDecimal prodQty;
    @NotNull private BigDecimal goodQty;
    private BigDecimal defectQty;
    @NotNull private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    private BigDecimal workTime;
    private BigDecimal setupTime;
    private BigDecimal downTime;
    private String lotNo;
    private String resultStatus;
    private String resultRmk;
}
