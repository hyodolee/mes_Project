package com.mes.domain.production.processresult.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProcessResultCreateRequest {
    private Long procResultId; // For useGeneratedKeys

    @NotNull private Long resultId;
    @NotNull private Long routingId;
    @NotNull private Integer processSeq;
    @NotBlank private String processCd;
    @NotBlank private String processNm;
    @NotNull private LocalDateTime startDtm;
    private LocalDateTime endDtm;
    @NotNull private BigDecimal inputQty;
    private BigDecimal outputQty;
    private BigDecimal goodQty;
    private BigDecimal defectQty;
    private String workerId;
    private String equipmentCd;
    private BigDecimal workTime;
    private String processStatus;
    private String lotNo;
    private String procRmk;
}
