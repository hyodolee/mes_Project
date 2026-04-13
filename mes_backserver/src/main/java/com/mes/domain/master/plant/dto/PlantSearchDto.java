package com.mes.domain.master.plant.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlantSearchDto extends PageRequest {
    private String plantNm;
    private String useYn;
}
