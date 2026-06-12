package com.mes.domain.ai.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderAiEvidenceDto {
    private WorkOrderEvidenceDto workOrder;
    private McsTransferEvidenceDto mcsTransfer;
    private List<PlcEventEvidenceDto> plcEvents;
}
