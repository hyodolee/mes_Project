package com.mes.mcs.application.service.zone;

import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.mcs.domain.zone.dto.ZoneDto;
import com.mes.mcs.domain.zone.dto.ZoneSearchDto;
import com.mes.mcs.infra.persistence.mybatis.mapper.zone.ZoneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("mcsZoneService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneMapper zoneMapper;

    public PageResponse<ZoneDto> getZoneList(ZoneSearchDto searchDto) {
        List<ZoneDto> list = zoneMapper.selectZoneList(searchDto);
        long total = zoneMapper.selectZoneCount(searchDto);
        return PageResponse.createPagedResponse(list, Math.toIntExact(total), searchDto);
    }

    public ZoneDto getZone(Long zoneId) {
        return zoneMapper.selectZoneById(zoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "존재하지 않는 Zone입니다."));
    }

    @Transactional
    public Long createZone(ZoneDto zoneDto) {
        zoneMapper.selectZoneByCd(zoneDto.getWarehouseCd(), zoneDto.getZoneCd())
                .ifPresent(z -> {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "이미 등록된 Zone 코드입니다.");
                });

        zoneMapper.insertZone(zoneDto);
        return zoneDto.getZoneId();
    }

    @Transactional
    public void updateZone(ZoneDto zoneDto) {
        getZone(zoneDto.getZoneId());
        zoneMapper.updateZone(zoneDto);
    }

    @Transactional
    public void deleteZone(Long zoneId) {
        getZone(zoneId);
        zoneMapper.deleteZone(zoneId);
    }
}
