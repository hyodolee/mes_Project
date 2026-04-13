package com.mcs.application.service.location;

import com.mcs.domain.location.dto.LocationDto;
import com.mcs.domain.location.dto.LocationSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.location.LocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationService {

    private final LocationMapper locationMapper;

    public PageResponse<LocationDto> getLocationList(LocationSearchDto searchDto) {
        List<LocationDto> list = locationMapper.selectLocationList(searchDto);
        long total = locationMapper.selectLocationCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public LocationDto getLocation(Long locationId) {
        return locationMapper.selectLocationById(locationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOCATION_NOT_FOUND));
    }

    @Transactional
    public Long createLocation(LocationDto locationDto) {
        // 중복 체크
        locationMapper.selectLocationByCd(locationDto.zoneId(), locationDto.locationCd())
                .ifPresent(l -> {
                    throw new BusinessException(ErrorCode.DUPLICATE_LOCATION_CD);
                });

        locationMapper.insertLocation(locationDto);
        return locationDto.locationId();
    }

    @Transactional
    public void updateLocation(LocationDto locationDto) {
        // 존재 확인
        getLocation(locationDto.locationId());
        locationMapper.updateLocation(locationDto);
    }

    @Transactional
    public void deleteLocation(Long locationId) {
        // 존재 확인
        getLocation(locationId);
        // TODO: 재고가 있는지 체크 로직 필요할 수 있음
        locationMapper.deleteLocation(locationId);
    }
}
