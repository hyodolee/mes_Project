package com.mes.domain.planning.workorder.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkOrderUpdateStatusRequest(
        @NotBlank(message = "작업상태는 필수입니다.")
        String woStatus
) {
}
