package com.mes.domain.quality.inspectresult.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class InspectDetailCreateRequest {
    @NotNull private Long inspectStdId;
    @NotNull private Integer sampleNo;
    @NotBlank private String inspectItem;
    @NotBlank private String dataType;
    private BigDecimal measureValue;
    private String measureText;
    private BigDecimal lsl;
    private BigDecimal usl;
    private BigDecimal targetValue;
    @NotBlank private String judgeResult;
    private String defectCd;
    private String detailRmk;
}
