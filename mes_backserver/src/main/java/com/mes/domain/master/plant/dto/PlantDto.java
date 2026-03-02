package com.mes.domain.master.plant.dto;

public record PlantDto(
        String plantCd,
        String companyCd,
        String companyNm,
        String plantNm,
        String addr,
        String telNo,
        String useYn
) {
}
