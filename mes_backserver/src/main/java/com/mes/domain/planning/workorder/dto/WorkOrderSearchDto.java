package com.mes.domain.planning.workorder.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class WorkOrderSearchDto extends PageRequest {
    private String plantCd;
    private String itemCd;
    private String woStatus;
    private LocalDate woFromDt;
    private LocalDate woToDt;
}
