package com.mes.domain.planning.prodplan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdPlanCreateRequest {

    @NotBlank(message = "공장코드는 필수입니다.")
    @Size(max = 20, message = "공장코드는 20자 이하여야 합니다.")
    private String plantCd;

    @NotBlank(message = "계획번호는 필수입니다.")
    @Size(max = 30, message = "계획번호는 30자 이하여야 합니다.")
    private String planNo;

    @NotNull(message = "계획일자는 필수입니다.")
    private LocalDate planDt;

    @NotBlank(message = "계획유형은 필수입니다.")
    @Size(max = 20, message = "계획유형은 20자 이하여야 합니다.")
    private String planType;

    @NotBlank(message = "품목코드는 필수입니다.")
    @Size(max = 50, message = "품목코드는 50자 이하여야 합니다.")
    private String itemCd;

    @NotNull(message = "계획수량은 필수입니다.")
    @DecimalMin(value = "0.001", message = "계획수량은 0.001 이상이어야 합니다.")
    private BigDecimal planQty;

    @NotNull(message = "계획시작일은 필수입니다.")
    private LocalDate planStartDt;

    @NotNull(message = "계획종료일은 필수입니다.")
    private LocalDate planEndDt;

    private Integer priority;

    @Size(max = 50, message = "수주번호는 50자 이하여야 합니다.")
    private String orderNo;

    @Size(max = 50, message = "고객코드는 50자 이하여야 합니다.")
    private String customerCd;

    @Size(max = 100, message = "고객명은 100자 이하여야 합니다.")
    private String customerNm;

    private LocalDate deliveryDt;

    @Size(max = 20, message = "계획상태는 20자 이하여야 합니다.")
    private String planStatus;

    @Size(max = 500, message = "비고는 500자 이하여야 합니다.")
    private String planRmk;
}
