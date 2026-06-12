package com.mes.domain.planning.workorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderCreateRequest {

    @NotBlank(message = "공장코드는 필수입니다.")
    @Size(max = 20, message = "공장코드는 20자 이하여야 합니다.")
    private String plantCd;

    private Long planId;

    @NotNull(message = "작업지시일은 필수입니다.")
    private LocalDate woDt;

    @NotBlank(message = "품목코드는 필수입니다.")
    @Size(max = 50, message = "품목코드는 50자 이하여야 합니다.")
    private String itemCd;

    @NotNull(message = "지시수량은 필수입니다.")
    @DecimalMin(value = "0.001", message = "지시수량은 0.001 이상이어야 합니다.")
    private BigDecimal woQty;

    @Size(max = 20, message = "작업장코드는 20자 이하여야 합니다.")
    @NotBlank(message = "작업장코드는 필수입니다.")
    private String workcenterCd;

    @Size(max = 20, message = "설비코드는 20자 이하여야 합니다.")
    private String equipmentCd;

    @Size(max = 20, message = "작업자ID는 20자 이하여야 합니다.")
    private String workerId;

    @NotNull(message = "계획시작일시는 필수입니다.")
    private LocalDateTime planStartDtm;

    @NotNull(message = "계획종료일시는 필수입니다.")
    private LocalDateTime planEndDtm;

    private Integer priority;

    @Size(max = 50, message = "LOT번호는 50자 이하여야 합니다.")
    private String lotNo;

    @Size(max = 50, message = "수주번호는 50자 이하여야 합니다.")
    private String orderNo;

    private LocalDate deliveryDt;

    @Size(max = 500, message = "비고는 500자 이하여야 합니다.")
    private String woRmk;
}
