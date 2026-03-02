package com.mes.domain.master.company.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompanyUpsertRequest(
        @NotBlank(message = "회사코드는 필수입니다.")
        @Size(max = 20, message = "회사코드는 20자 이하여야 합니다.")
        String companyCd,
        @NotBlank(message = "회사명은 필수입니다.")
        @Size(max = 100, message = "회사명은 100자 이하여야 합니다.")
        String companyNm,
        @Size(max = 20, message = "사업자번호는 20자 이하여야 합니다.")
        String bizNo,
        @Size(max = 50, message = "대표자명은 50자 이하여야 합니다.")
        String ceoNm,
        @Size(max = 500, message = "주소는 500자 이하여야 합니다.")
        String addr,
        @Size(max = 20, message = "전화번호는 20자 이하여야 합니다.")
        String telNo,
        @Pattern(regexp = "Y|N", message = "사용여부는 Y 또는 N만 입력할 수 있습니다.")
        String useYn
) {
}
