package com.mes.domain.quality.inspectstd.dto;

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
public class InspectStdDto {
    private Long inspectStdId;
    private String plantCd;
    private String itemCd;
    private String inspectType;
    private Integer inspectItemSeq;
    private String inspectItem;
    private String inspectMethod;
    private String dataType;
    private String specValue;
    private BigDecimal lsl;
    private BigDecimal usl;
    private BigDecimal targetValue;
    private String unit;
    private Integer sampleSize;
    private String sampleMethod;
    private String inspectTool;
    private String mandatoryYn;
    private String processCd;
    private LocalDate validStartDt;
    private LocalDate validEndDt;
    private String stdRmk;
    private String useYn;
    private String regUserId;
    private LocalDateTime regDtm;
}
