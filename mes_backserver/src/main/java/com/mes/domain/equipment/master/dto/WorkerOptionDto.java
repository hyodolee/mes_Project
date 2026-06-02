package com.mes.domain.equipment.master.dto;

public record WorkerOptionDto(
        String workerId,
        String plantCd,
        String workerNm,
        String deptCd,
        String position
) {
}
