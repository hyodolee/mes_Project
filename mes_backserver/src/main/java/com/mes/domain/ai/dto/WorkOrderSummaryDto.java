package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderSummaryDto {
    private long total;
    private long inProgress;
    private long delayed;
    private long pending;
    private long completedToday;
}
