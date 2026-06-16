package com.mes.mcs.application.service.location;

import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.mcs.domain.location.dto.LocationDto;
import com.mes.mcs.domain.location.dto.LocationSearchDto;
import com.mes.mcs.infra.persistence.mybatis.mapper.location.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("mcsLocationService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationMapper locationMapper;

    public PageResponse<LocationDto> getLocationList(LocationSearchDto searchDto) {
        List<LocationDto> list = locationMapper.selectLocationList(searchDto);
        long total = locationMapper.selectLocationCount(searchDto);
        return PageResponse.createPagedResponse(list, Math.toIntExact(total), searchDto);
    }

    public LocationDto getLocation(Long locationId) {
        return locationMapper.selectLocationById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_ERROR, "존재하지 않는 Location입니다."));
    }

    @Transactional
    public Long createLocation(LocationDto locationDto) {
        locationMapper.selectLocationByCd(locationDto.getZoneId(), locationDto.getLocationCd())
                .ifPresent(l -> {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR, "이미 등록된 Location 코드입니다.");
                });

        locationMapper.insertLocation(locationDto);
        return locationDto.getLocationId();
    }

    @Transactional
    public void updateLocation(LocationDto locationDto) {
        getLocation(locationDto.getLocationId());
        locationMapper.updateLocation(locationDto);
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        getLocation(locationId);
        locationMapper.deleteLocation(locationId);
    }
}
