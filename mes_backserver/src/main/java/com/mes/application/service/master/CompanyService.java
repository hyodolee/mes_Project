package com.mes.application.service.master;

import com.mes.domain.master.company.dto.CompanyDto;
import com.mes.domain.master.company.dto.CompanyUpsertRequest;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.master.CompanyMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final CompanyMapper companyMapper;

    public CompanyService(CompanyMapper companyMapper) {
        this.companyMapper = companyMapper;
    }

    public List<CompanyDto> getCompanies(String companyNm, String useYn) {
        return companyMapper.selectCompanies(companyNm, useYn);
    }

    public CompanyDto getCompany(String companyCd) {
        CompanyDto company = companyMapper.selectCompanyById(companyCd);
        if (company == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "회사를 찾을 수 없습니다. companyCd=" + companyCd);
        }
        return company;
    }

    public void createCompany(CompanyUpsertRequest request) {
        int inserted = companyMapper.insertCompany(request, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "회사 등록에 실패했습니다.");
        }
    }

    public void updateCompany(CompanyUpsertRequest request) {
        int updated = companyMapper.updateCompany(request, SYSTEM_USER);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "회사 수정에 실패했습니다.");
        }
    }
}
