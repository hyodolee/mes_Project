package com.mes.domain.master.bom.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BomSearchDto extends PageRequest {
    private String parentItemCd; // 모품목으로 필터
    private String keyword;       // 품목코드/품목명 검색
    private String useYn;
}
