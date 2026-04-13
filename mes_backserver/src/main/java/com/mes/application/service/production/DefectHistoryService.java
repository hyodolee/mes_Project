package com.mes.application.service.production;

import com.mes.domain.production.defect.dto.DefectHistoryCreateRequest;
import com.mes.domain.production.defect.dto.DefectHistoryDto;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.production.DefectHistoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DefectHistoryService {

    private static final String SYSTEM_USER = "SYSTEM";
    private final DefectHistoryMapper defectHistoryMapper;

    public DefectHistoryService(DefectHistoryMapper defectHistoryMapper) {
        this.defectHistoryMapper = defectHistoryMapper;
    }

    public List<DefectHistoryDto> getDefectHistories(String plantCd, String itemCd, LocalDate fromDt, LocalDate toDt) {
        return defectHistoryMapper.selectDefectHistories(plantCd, itemCd, fromDt, toDt);
    }

    @Transactional
    public void createDefectHistory(DefectHistoryCreateRequest request) {
        int inserted = defectHistoryMapper.insertDefectHistory(request, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "불량이력 등록에 실패했습니다.");
        }
    }
}
