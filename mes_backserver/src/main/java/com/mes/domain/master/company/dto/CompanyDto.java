package com.mes.domain.master.company.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
    private String companyCd;
    private String companyNm;
    private String bizNo;
    private String ceoNm;
    private String addr;
    private String telNo;
    private String useYn;
}
