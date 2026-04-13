package com.mes.application.service.quality;

import com.mes.domain.quality.inspectresult.dto.InspectDetailCreateRequest;
import com.mes.domain.quality.inspectresult.dto.InspectResultCreateRequest;
import com.mes.domain.quality.inspectresult.dto.InspectResultDto;
import com.mes.domain.quality.inspectresult.dto.InspectResultSearchDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.quality.InspectResultMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class InspectResultService {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final DateTimeFormatter INSPECT_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final InspectResultMapper inspectResultMapper;

    public InspectResultService(InspectResultMapper inspectResultMapper) {
        this.inspectResultMapper = inspectResultMapper;
    }

    public List<InspectResultDto> getInspectResults(String plantCd, String itemCd, LocalDate fromDt, LocalDate toDt) {
        return inspectResultMapper.selectInspectResults(plantCd, itemCd, fromDt, toDt);
    }

    public PageResponse<InspectResultDto> getInspectResultPage(InspectResultSearchDto searchDto) {
        int total = inspectResultMapper.countInspectResults(searchDto);
        List<InspectResultDto> content = inspectResultMapper.selectInspectResultList(searchDto);
        return PageResponse.createPagedResponse(content, total, searchDto);
    }

    public InspectResultDto getInspectResult(Long inspectId) {
        InspectResultDto result = inspectResultMapper.selectInspectResultById(inspectId);
        if (result == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "검사실적을 찾을 수 없습니다. inspectId=" + inspectId);
        }
        return result;
    }

    @Transactional
    public void createInspectResult(InspectResultCreateRequest request) {
        // 1. Generate Inspect No
        String inspectNo = generateInspectNo(request.getInspectDt());

        // 2. Insert Inspect Result
        int inserted = inspectResultMapper.insertInspectResult(request, inspectNo, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "검사실적 등록에 실패했습니다.");
        }

        // inspectId is now populated in request object by useGeneratedKeys
        Long inspectId = request.getInspectId();

        // 3. Insert Details
        if (request.getDetails() != null) {
            for (InspectDetailCreateRequest detail : request.getDetails()) {
                inspectResultMapper.insertInspectDetail(inspectId, detail, SYSTEM_USER);
            }
        }
    }

    private String generateInspectNo(LocalDate inspectDt) {
        int count = inspectResultMapper.selectInspectResultCountByDate(inspectDt);
        return "QC" + inspectDt.format(INSPECT_DATE_FMT) + String.format("%04d", count + 1);
    }
}
