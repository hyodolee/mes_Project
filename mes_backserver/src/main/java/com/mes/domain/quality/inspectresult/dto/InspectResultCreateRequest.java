package com.mes.domain.quality.inspectresult.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class InspectResultCreateRequest {
    private Long inspectId; // For useGeneratedKeys

    @NotBlank private String plantCd;
    @NotBlank private String inspectType;
    @NotNull private LocalDate inspectDt;
    @NotBlank private String itemCd;
    private String lotNo;
    @NotNull private BigDecimal inspectQty;
    private BigDecimal sampleQty;
    private BigDecimal passQty;
    private BigDecimal failQty;
    @NotBlank private String inspectorId;
    private Long woId;
    private Long resultId;
    private Long receiveId;
    private String vendorCd;
    @NotBlank private String judgeResult;
    private String judgeUserId;
    private LocalDateTime judgeDtm;
    private String processCd;
    private String inspectRmk;
    @NotEmpty @Valid private List<InspectDetailCreateRequest> details;
}
