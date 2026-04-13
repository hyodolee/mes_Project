package com.mes.domain.master.company.dto;

import com.mes.global.common.dto.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanySearchDto extends PageRequest {
    private String companyNm;
    private String useYn;
}
