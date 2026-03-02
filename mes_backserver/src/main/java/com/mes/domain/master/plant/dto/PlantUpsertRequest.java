package com.mes.domain.master.plant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PlantUpsertRequest(
        @NotBlank(message = "공장코드는 필수입니다.")
        @Size(max = 20, message = "공장코드는 20자 이하여야 합니다.")
        String plantCd,
        @NotBlank(message = "회사코드는 필수입니다.")
        @Size(max = 20, message = "회사코드는 20자 이하여야 합니다.")
        String companyCd,
        @NotBlank(message = "공장명은 필수입니다.")
        @Size(max = 100, message = "공장명은 100자 이하여야 합니다.")
        String plantNm,
        @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
        String addr,
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String telNo,
        @Pattern(regexp = "Y|N", message = "사용여부는 Y 또는 N만 입력할 수 있습니다.")
        String useYn
) {
}
