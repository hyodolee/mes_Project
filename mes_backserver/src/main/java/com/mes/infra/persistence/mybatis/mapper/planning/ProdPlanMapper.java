package com.mes.infra.persistence.mybatis.mapper.planning;

import com.mes.domain.planning.prodplan.dto.ProdPlanCreateRequest;
import com.mes.domain.planning.prodplan.dto.ProdPlanDto;
import com.mes.domain.planning.prodplan.dto.ProdPlanSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ProdPlanMapper {
    List<ProdPlanDto> selectProdPlans(
            @Param("plantCd") String plantCd,
            @Param("itemCd") String itemCd,
            @Param("planStatus") String planStatus,
            @Param("planFromDt") LocalDate planFromDt,
            @Param("planToDt") LocalDate planToDt
    );

    List<ProdPlanDto> selectProdPlanList(ProdPlanSearchDto searchDto);

    int countProdPlans(ProdPlanSearchDto searchDto);

    ProdPlanDto selectProdPlanById(@Param("planId") Long planId);

    int insertProdPlan(@Param("request") ProdPlanCreateRequest request, @Param("regUserId") String regUserId);
}
