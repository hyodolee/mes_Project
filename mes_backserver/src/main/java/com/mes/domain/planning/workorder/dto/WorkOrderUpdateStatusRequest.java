package com.mes.domain.planning.workorder.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderUpdateStatusRequest {

    @NotBlank(message = "작업상태는 필수입니다.")
    private String woStatus;
}
