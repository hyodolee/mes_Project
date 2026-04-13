package com.mes.domain.equipment.oper.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class OperStatusSearchDto extends PageRequest {
    private String plantCd;
    private String equipmentCd;
    private LocalDate fromDt;
    private LocalDate toDt;
}
