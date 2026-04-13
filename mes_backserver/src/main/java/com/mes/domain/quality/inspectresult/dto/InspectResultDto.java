package com.mes.domain.quality.inspectresult.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectResultDto {
    private Long inspectId;
    private String plantCd;
    private String inspectNo;
    private String inspectType;
    private LocalDate inspectDt;
    private String itemCd;
    private String lotNo;
    private BigDecimal inspectQty;
    private BigDecimal sampleQty;
    private BigDecimal passQty;
    private BigDecimal failQty;
    private String inspectorId;
    private Long woId;
    private Long resultId;
    private Long receiveId;
    private String vendorCd;
    private String judgeResult;
    private String judgeUserId;
    private LocalDateTime judgeDtm;
    private String processCd;
    private String inspectRmk;
    private String regUserId;
    private LocalDateTime regDtm;
    
    @Builder.Default
    private List<InspectDetailDto> details = new ArrayList<>();
}
