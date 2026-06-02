package com.mes.domain.equipment.master.dto;

public record EquipmentOptionDto(
        String equipmentCd,
        String plantCd,
        String workcenterCd,
        String equipmentNm,
        String equipmentType,
        String equipmentStatus
) {
}
