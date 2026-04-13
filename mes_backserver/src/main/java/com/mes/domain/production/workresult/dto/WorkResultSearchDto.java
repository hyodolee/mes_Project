package com.mes.domain.production.workresult.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class WorkResultSearchDto extends PageRequest {
    private String plantCd;
    private String itemCd;
    private LocalDate resultFromDt;
    private LocalDate resultToDt;
}
