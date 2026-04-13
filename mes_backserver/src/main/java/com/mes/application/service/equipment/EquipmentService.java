package com.mes.application.service.equipment;

import com.mes.domain.equipment.oper.dto.OperStatusDto;
import com.mes.domain.equipment.oper.dto.OperStatusRequest;
import com.mes.domain.equipment.oper.dto.OperStatusSearchDto;
import com.mes.domain.equipment.downtime.dto.DowntimeDto;
import com.mes.domain.equipment.downtime.dto.DowntimeRequest;
import com.mes.domain.equipment.maint.dto.MaintHisDto;
import com.mes.domain.equipment.maint.dto.MaintHisRequest;
import com.mes.global.common.dto.PageResponse;
import com.mes.infra.persistence.mybatis.mapper.equipment.EquipmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipmentService {

    private static final DateTimeFormatter MAINT_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EquipmentMapper equipmentMapper;

    public EquipmentService(EquipmentMapper equipmentMapper) {
        this.equipmentMapper = equipmentMapper;
    }

    // Operation Status
    public List<OperStatusDto> getOperStatuses(String plantCd, String equipmentCd, LocalDate fromDt, LocalDate toDt) {
        return equipmentMapper.selectOperStatuses(plantCd, equipmentCd, fromDt, toDt);
    }

    public PageResponse<OperStatusDto> getOperStatusPage(OperStatusSearchDto searchDto) {
        int total = equipmentMapper.countOperStatuses(searchDto);
        List<OperStatusDto> content = equipmentMapper.selectOperStatusList(searchDto);
        return PageResponse.createPagedResponse(content, total, searchDto);
    }

    @Transactional
    public void recordOperStatus(OperStatusRequest request) {
        equipmentMapper.insertOperStatus(request);
    }

    // Downtime
    public List<DowntimeDto> getDowntimes(String plantCd, String equipmentCd, LocalDate fromDt, LocalDate toDt) {
        return equipmentMapper.selectDowntimes(plantCd, equipmentCd, fromDt, toDt);
    }

    @Transactional
    public void recordDowntime(DowntimeRequest request) {
        equipmentMapper.insertDowntime(request);
    }

    // Maintenance
    public List<MaintHisDto> getMaintHistories(String plantCd, String equipmentCd, LocalDate fromDt, LocalDate toDt) {
        return equipmentMapper.selectMaintHistories(plantCd, equipmentCd, fromDt, toDt);
    }

    @Transactional
    public void recordMaintHistory(MaintHisRequest request) {
        String maintNo = generateMaintNo(request.getMaintDt());
        equipmentMapper.insertMaintHistory(request, maintNo);
    }

    private String generateMaintNo(LocalDate maintDt) {
        int count = equipmentMapper.selectMaintCountByDate(maintDt);
        return "MT" + maintDt.format(MAINT_DATE_FMT) + String.format("%04d", count + 1);
    }
}
