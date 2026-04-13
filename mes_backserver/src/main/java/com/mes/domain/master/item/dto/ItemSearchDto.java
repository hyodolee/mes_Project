package com.mes.domain.master.item.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemSearchDto extends PageRequest {
    private String itemNm;
    private String useYn;
}
