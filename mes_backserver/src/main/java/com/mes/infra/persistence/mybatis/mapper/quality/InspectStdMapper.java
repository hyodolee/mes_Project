package com.mes.infra.persistence.mybatis.mapper.quality;

import com.mes.domain.quality.inspectstd.dto.InspectStdDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InspectStdMapper {
    List<InspectStdDto> selectInspectStds(
        @Param("plantCd") String plantCd,
        @Param("itemCd") String itemCd,
        @Param("inspectType") String inspectType
    );
}
