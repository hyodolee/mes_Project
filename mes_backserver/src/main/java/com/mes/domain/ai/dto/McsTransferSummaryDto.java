package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class McsTransferSummaryDto {
    private long active;
    private long failed;
    private long completedToday;
}
