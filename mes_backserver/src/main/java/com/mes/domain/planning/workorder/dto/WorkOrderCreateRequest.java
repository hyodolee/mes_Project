package com.mes.domain.planning.workorder.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WorkOrderCreateRequest(
        @NotBlank(message = "공장코드는 필수입니다.")
        @Size(max = 20, message = "공장코드는 20자 이하여야 합니다.")
        String plantCd,
        Long planId,
        @NotNull(message = "작업지시일은 필수입니다.")
        LocalDate woDt,
        @NotBlank(message = "품목코드는 필수입니다.")
        @Size(max = 50, message = "품목코드는 50자 이하여야 합니다.")
        String itemCd,
        @NotNull(message = "지시수량은 필수입니다.")
        @DecimalMin(value = "0.001", message = "지시수량은 0.001 이상이어야 합니다.")
        BigDecimal woQty,
        @Size(max = 20, message = "작업장코드는 20자 이하여야 합니다.")
        String workcenterCd,
        @Size(max = 20, message = "설비코드는 20자 이하여야 합니다.")
        String equipmentCd,
        @Size(max = 20, message = "작업자ID는 20자 이하여야 합니다.")
        String workerId,
        @NotNull(message = "계획시작일시는 필수입니다.")
        LocalDateTime planStartDtm,
        @NotNull(message = "계획종료일시는 필수입니다.")
        LocalDateTime planEndDtm,
        Integer priority,
        @Size(max = 50, message = "LOT번호는 50자 이하여야 합니다.")
        String lotNo,
        @Size(max = 50, message = "수주번호는 50자 이하여야 합니다.")
        String orderNo,
        LocalDate deliveryDt,
        @Size(max = 500, message = "비고는 500자 이하여야 합니다.")
        String woRmk
) {
}
