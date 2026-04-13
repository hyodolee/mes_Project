package com.mes.infra.persistence.mybatis.mapper.production;

import com.mes.domain.production.defect.dto.DefectHistoryCreateRequest;
import com.mes.domain.production.defect.dto.DefectHistoryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DefectHistoryMapper {
    List<DefectHistoryDto> selectDefectHistories(
        @Param("plantCd") String plantCd,
        @Param("itemCd") String itemCd,
        @Param("defectFromDt") LocalDate defectFromDt,
        @Param("defectToDt") LocalDate defectToDt
    );

    int insertDefectHistory(
        @Param("request") DefectHistoryCreateRequest request,
        @Param("regUserId") String regUserId
    );
}
