package com.mes.infra.persistence.mybatis.mapper.master;

import com.mes.domain.master.company.dto.CompanyDto;
import com.mes.domain.master.company.dto.CompanySearchDto;
import com.mes.domain.master.company.dto.CompanyUpsertRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompanyMapper {
    List<CompanyDto> selectCompanies(@Param("companyNm") String companyNm, @Param("useYn") String useYn);

    List<CompanyDto> selectCompanyList(CompanySearchDto searchDto);

    int countCompanies(CompanySearchDto searchDto);

    CompanyDto selectCompanyById(@Param("companyCd") String companyCd);

    int insertCompany(@Param("request") CompanyUpsertRequest request, @Param("regUserId") String regUserId);

    int updateCompany(@Param("request") CompanyUpsertRequest request, @Param("updUserId") String updUserId);
}
