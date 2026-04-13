package com.mes.infra.persistence.mybatis.mapper.production;

import com.mes.domain.production.workresult.dto.WorkResultCreateRequest;
import com.mes.domain.production.workresult.dto.WorkResultDto;
import com.mes.domain.production.workresult.dto.WorkResultSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WorkResultMapper {
    List<WorkResultDto> selectWorkResults(
        @Param("plantCd") String plantCd,
        @Param("itemCd") String itemCd,
        @Param("resultFromDt") LocalDate resultFromDt,
        @Param("resultToDt") LocalDate resultToDt
    );

    List<WorkResultDto> selectWorkResultList(WorkResultSearchDto searchDto);

    int countWorkResults(WorkResultSearchDto searchDto);

    int insertWorkResult(
        @Param("request") WorkResultCreateRequest request,
        @Param("resultNo") String resultNo,
        @Param("regUserId") String regUserId
    );

    int selectWorkResultCountByDate(@Param("resultDt") LocalDate resultDt);
}
