package com.mes.interfaces.api.master;

import com.mes.application.service.master.CompanyService;
import com.mes.domain.master.company.dto.CompanyDto;
import com.mes.domain.master.company.dto.CompanyUpsertRequest;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master/companies")
public class CompanyApiController {

    private final CompanyService companyService;

    public CompanyApiController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public ApiResponse<List<CompanyDto>> getCompanies(
            @RequestParam(name = "companyNm", required = false) String companyNm,
            @RequestParam(name = "useYn", required = false) String useYn
    ) {
        return ApiResponse.ok(companyService.getCompanies(companyNm, useYn));
    }

    @GetMapping("/{companyCd}")
    public ApiResponse<CompanyDto> getCompany(@PathVariable("companyCd") String companyCd) {
        return ApiResponse.ok(companyService.getCompany(companyCd));
    }

    @PostMapping
    public ApiResponse<Void> createCompany(@Valid @RequestBody CompanyUpsertRequest request) {
        companyService.createCompany(request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{companyCd}")
    public ApiResponse<Void> updateCompany(@PathVariable("companyCd") String companyCd, @Valid @RequestBody CompanyUpsertRequest request) {
        if (!companyCd.equals(request.companyCd())) {
            throw new IllegalArgumentException("경로의 companyCd와 요청 본문의 companyCd가 일치하지 않습니다.");
        }
        companyService.updateCompany(request);
        return ApiResponse.ok(null);
    }
}
