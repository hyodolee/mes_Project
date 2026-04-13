package com.mes.application.service.production;

import com.mes.domain.production.processresult.dto.ProcessResultCreateRequest;
import com.mes.domain.production.processresult.dto.ProcessResultDto;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.infra.persistence.mybatis.mapper.production.ProcessResultMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProcessResultService {

    private static final String SYSTEM_USER = "SYSTEM";
    private final ProcessResultMapper processResultMapper;

    public ProcessResultService(ProcessResultMapper processResultMapper) {
        this.processResultMapper = processResultMapper;
    }

    public List<ProcessResultDto> getProcessResults(Long resultId) {
        return processResultMapper.selectProcessResults(resultId);
    }

    @Transactional
    public void createProcessResult(ProcessResultCreateRequest request) {
        int inserted = processResultMapper.insertProcessResult(request, SYSTEM_USER);
        if (inserted != 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "공정실적 등록에 실패했습니다.");
        }
    }
}
