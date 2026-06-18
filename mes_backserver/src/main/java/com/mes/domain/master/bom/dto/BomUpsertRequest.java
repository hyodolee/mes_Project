package com.mes.domain.master.bom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BomUpsertRequest {

    private Long bomId; // 수정 시 사용

    @NotBlank(message = "공장코드는 필수입니다.")
    private String plantCd;

    @NotBlank(message = "모품목코드는 필수입니다.")
    private String parentItemCd;

    @NotBlank(message = "자품목코드는 필수입니다.")
    private String childItemCd;

    private Integer bomLevel;

    @NotNull(message = "소요량은 필수입니다.")
    private Double bomQty;

    private Double lossRate;

    @NotBlank(message = "유효시작일은 필수입니다.")
    private String startDt; // yyyy-MM-dd

    private String endDt;
    private String processCd;
    private String bomRmk;
    private String useYn;
}
