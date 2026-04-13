package com.mes.infra.persistence.mybatis.mapper.production;

import com.mes.domain.production.processresult.dto.ProcessResultCreateRequest;
import com.mes.domain.production.processresult.dto.ProcessResultDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProcessResultMapper {
    List<ProcessResultDto> selectProcessResults(@Param("resultId") Long resultId);

    int insertProcessResult(
        @Param("request") ProcessResultCreateRequest request,
        @Param("regUserId") String regUserId
    );
}
