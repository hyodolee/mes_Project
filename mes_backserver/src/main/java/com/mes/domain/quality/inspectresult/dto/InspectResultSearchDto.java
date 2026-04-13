package com.mes.domain.quality.inspectresult.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class InspectResultSearchDto extends PageRequest {
    private String plantCd;
    private String itemCd;
    private LocalDate inspectFromDt;
    private LocalDate inspectToDt;
}
