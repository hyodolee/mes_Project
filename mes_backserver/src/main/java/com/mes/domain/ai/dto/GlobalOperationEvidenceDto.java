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
public class GlobalOperationEvidenceDto {
    private WorkOrderSummaryDto workOrders;
    private McsTransferSummaryDto transfers;
    private List<CriticalEventDto> criticalEvents;
}
