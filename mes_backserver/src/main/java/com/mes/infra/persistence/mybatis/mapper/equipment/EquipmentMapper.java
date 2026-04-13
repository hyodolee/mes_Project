package com.mes.infra.persistence.mybatis.mapper.equipment;

import com.mes.domain.equipment.oper.dto.OperStatusDto;
import com.mes.domain.equipment.oper.dto.OperStatusRequest;
import com.mes.domain.equipment.oper.dto.OperStatusSearchDto;
import com.mes.domain.equipment.downtime.dto.DowntimeDto;
import com.mes.domain.equipment.downtime.dto.DowntimeRequest;
import com.mes.domain.equipment.maint.dto.MaintHisDto;
import com.mes.domain.equipment.maint.dto.MaintHisRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EquipmentMapper {
    // Operation Status
    List<OperStatusDto> selectOperStatuses(
        @Param("plantCd") String plantCd,
        @Param("equipmentCd") String equipmentCd,
        @Param("fromDt") LocalDate fromDt,
        @Param("toDt") LocalDate toDt
    );
    
    List<OperStatusDto> selectOperStatusList(OperStatusSearchDto searchDto);
    
    int countOperStatuses(OperStatusSearchDto searchDto);
    
    int insertOperStatus(@Param("request") OperStatusRequest request);

    // Downtime
    List<DowntimeDto> selectDowntimes(
        @Param("plantCd") String plantCd,
        @Param("equipmentCd") String equipmentCd,
        @Param("fromDt") LocalDate fromDt,
        @Param("toDt") LocalDate toDt
    );
    int insertDowntime(@Param("request") DowntimeRequest request);

    // Maintenance
    List<MaintHisDto> selectMaintHistories(
        @Param("plantCd") String plantCd,
        @Param("equipmentCd") String equipmentCd,
        @Param("fromDt") LocalDate fromDt,
        @Param("toDt") LocalDate toDt
    );
    int insertMaintHistory(@Param("request") MaintHisRequest request, @Param("maintNo") String maintNo);
    int selectMaintCountByDate(@Param("maintDt") LocalDate maintDt);
}
