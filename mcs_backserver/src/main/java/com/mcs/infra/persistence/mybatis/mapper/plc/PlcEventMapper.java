package com.mcs.infra.persistence.mybatis.mapper.plc;

import com.mcs.domain.plc.dto.PlcEventDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PlcEventMapper {
    void insertPlcEvent(PlcEventDto eventDto);

    void updateProcessResult(
            @Param("eventId") Long eventId,
            @Param("processResult") String processResult,
            @Param("processMessage") String processMessage
    );
}
