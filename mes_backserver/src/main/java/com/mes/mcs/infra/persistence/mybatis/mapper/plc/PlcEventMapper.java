package com.mes.mcs.infra.persistence.mybatis.mapper.plc;

import com.mes.mcs.domain.plc.dto.PlcEventDto;
import com.mes.mcs.domain.plc.dto.PlcEventSearchDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PlcEventMapper {
    List<PlcEventDto> selectPlcEventList(PlcEventSearchDto searchDto);
    long selectPlcEventCount(PlcEventSearchDto searchDto);

    void insertPlcEvent(PlcEventDto eventDto);

    void updateProcessResult(
            @Param("eventId") Long eventId,
            @Param("processResult") String processResult,
            @Param("processMessage") String processMessage
    );
}
