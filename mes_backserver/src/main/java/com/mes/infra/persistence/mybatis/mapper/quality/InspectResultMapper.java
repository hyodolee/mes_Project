package com.mes.infra.persistence.mybatis.mapper.quality;

import com.mes.domain.quality.inspectresult.dto.InspectDetailCreateRequest;
import com.mes.domain.quality.inspectresult.dto.InspectResultCreateRequest;
import com.mes.domain.quality.inspectresult.dto.InspectResultDto;
import com.mes.domain.quality.inspectresult.dto.InspectResultSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InspectResultMapper {
    List<InspectResultDto> selectInspectResults(
        @Param("plantCd") String plantCd,
        @Param("itemCd") String itemCd,
        @Param("inspectFromDt") LocalDate inspectFromDt,
        @Param("inspectToDt") LocalDate inspectToDt
    );

    List<InspectResultDto> selectInspectResultList(InspectResultSearchDto searchDto);

    int countInspectResults(InspectResultSearchDto searchDto);

    InspectResultDto selectInspectResultById(@Param("inspectId") Long inspectId);

    int insertInspectResult(
        @Param("request") InspectResultCreateRequest request,
        @Param("inspectNo") String inspectNo,
        @Param("regUserId") String regUserId
    );

    int insertInspectDetail(
        @Param("inspectId") Long inspectId,
        @Param("detail") InspectDetailCreateRequest detail,
        @Param("regUserId") String regUserId
    );

    int selectInspectResultCountByDate(@Param("inspectDt") LocalDate inspectDt);
}
