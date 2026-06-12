package com.mes.domain.master.plant.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlantDto {
    private String plantCd;
    private String companyCd;
    private String companyNm;
    private String plantNm;
    private String addr;
    private String telNo;
    private String useYn;
}
