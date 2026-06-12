package com.mes.domain.equipment.master.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentOptionDto {
    private String equipmentCd;
    private String plantCd;
    private String workcenterCd;
    private String equipmentNm;
    private String equipmentType;
    private String equipmentStatus;
}
