package com.mes.domain.quality.inspectresult.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectDetailDto {
    private Long inspectDtlId;
    private Long inspectId;
    private Long inspectStdId;
    private Integer sampleNo;
    private String inspectItem;
    private String dataType;
    private BigDecimal measureValue;
    private String measureText;
    private BigDecimal lsl;
    private BigDecimal usl;
    private BigDecimal targetValue;
    private String judgeResult;
    private String defectCd;
    private String detailRmk;
    private String regUserId;
    private LocalDateTime regDtm;
}
