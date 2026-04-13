package com.mcs.application.service.zone;

import com.mcs.domain.zone.dto.ZoneDto;
import com.mcs.domain.zone.dto.ZoneSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.zone.ZoneMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ZoneService {

    private final ZoneMapper zoneMapper;

    public PageResponse<ZoneDto> getZoneList(ZoneSearchDto searchDto) {
        List<ZoneDto> list = zoneMapper.selectZoneList(searchDto);
        long total = zoneMapper.selectZoneCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public ZoneDto getZone(Long zoneId) {
        return zoneMapper.selectZoneById(zoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ZONE_NOT_FOUND));
    }

    @Transactional
    public Long createZone(ZoneDto zoneDto) {
        // 중복 체크
        zoneMapper.selectZoneByCd(zoneDto.warehouseCd(), zoneDto.zoneCd())
                .ifPresent(z -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_ZONE_CD);
                });

        zoneMapper.insertZone(zoneDto);
        return zoneDto.zoneId();
    }

    @Transactional
    public void updateZone(ZoneDto zoneDto) {
        // 존재 확인
        getZone(zoneDto.zoneId());
        zoneMapper.updateZone(zoneDto);
    }

    @Transactional
    public void deleteZone(Long zoneId) {
        // 존재 확인
        getZone(zoneId);
        // TODO: 하위 로케이션이 있는지 체크 로직 필요할 수 있음
        zoneMapper.deleteZone(zoneId);
    }
}
