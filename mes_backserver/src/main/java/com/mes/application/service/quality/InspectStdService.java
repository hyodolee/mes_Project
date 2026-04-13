package com.mes.application.service.quality;

import com.mes.domain.quality.inspectstd.dto.InspectStdDto;
import com.mes.infra.persistence.mybatis.mapper.quality.InspectStdMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InspectStdService {

    private final InspectStdMapper inspectStdMapper;

    public InspectStdService(InspectStdMapper inspectStdMapper) {
        this.inspectStdMapper = inspectStdMapper;
    }

    public List<InspectStdDto> getInspectStds(String plantCd, String itemCd, String inspectType) {
        return inspectStdMapper.selectInspectStds(plantCd, itemCd, inspectType);
    }
}
