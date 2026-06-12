package com.mes.domain.master.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemUpsertRequest {

    @NotBlank(message = "품목코드는 필수입니다.")
    @Size(max = 20, message = "품목코드는 20자 이하여야 합니다.")
    private String itemCd;

    @NotBlank(message = "공장코드는 필수입니다.")
    private String plantCd;

    @NotBlank(message = "품목명은 필수입니다.")
    @Size(max = 100, message = "품목명은 100자 이하여야 합니다.")
    private String itemNm;

    @Size(max = 100, message = "규격은 100자 이하여야 합니다.")
    private String itemSpec;

    @NotBlank(message = "단위는 필수입니다.")
    @Size(max = 10, message = "단위는 10자 이하여야 합니다.")
    private String unit;

    @NotBlank(message = "품목유형은 필수입니다.")
    private String itemType;

    @Size(max = 20, message = "품목그룹은 20자 이하여야 합니다.")
    private String itemGrp;

    @NotBlank(message = "사용여부는 필수입니다.")
    @Size(min = 1, max = 1, message = "사용여부는 Y 또는 N이어야 합니다.")
    private String useYn;
}
