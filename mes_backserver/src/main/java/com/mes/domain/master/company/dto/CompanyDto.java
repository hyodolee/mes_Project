package com.mes.domain.master.company.dto;

public record CompanyDto(
        String companyCd,
        String companyNm,
        String bizNo,
        String ceoNm,
        String addr,
        String telNo,
        String useYn
) {
}
