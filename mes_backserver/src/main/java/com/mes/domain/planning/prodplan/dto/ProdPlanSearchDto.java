package com.mes.domain.planning.prodplan.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProdPlanSearchDto extends PageRequest {
    private String plantCd;
    private String itemCd;
    private String planStatus;
    private LocalDate planFromDt;
    private LocalDate planToDt;
}
