package com.mes.domain.equipment.master.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkerOptionDto {
    private String workerId;
    private String plantCd;
    private String workerNm;
    private String deptCd;
    private String position;
}
