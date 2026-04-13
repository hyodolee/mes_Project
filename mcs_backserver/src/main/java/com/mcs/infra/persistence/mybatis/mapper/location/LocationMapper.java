package com.mcs.infra.persistence.mybatis.mapper.location;

import com.mcs.domain.location.dto.LocationDto;
import com.mcs.domain.location.dto.LocationSearchDto;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Optional;

@Mapper
public interface LocationMapper {
    List<LocationDto> selectLocationList(LocationSearchDto searchDto);
    long selectLocationCount(LocationSearchDto searchDto);
    Optional<LocationDto> selectLocationById(Long locationId);
    Optional<LocationDto> selectLocationByCd(Long zoneId, String locationCd);
    void insertLocation(LocationDto locationDto);
    void updateLocation(LocationDto locationDto);
    void deleteLocation(Long locationId);
}
