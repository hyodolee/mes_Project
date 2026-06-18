package com.mes.mcs.interfaces.api.location;

import com.mes.global.common.dto.PageResponse;
import com.mes.global.response.ApiResponse;
import com.mes.mcs.application.service.location.LocationService;
import com.mes.mcs.domain.location.dto.LocationDto;
import com.mes.mcs.domain.location.dto.LocationSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("mcsLocationApiController")
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationApiController {

    private final LocationService locationService;

    @GetMapping
    public ApiResponse<PageResponse<LocationDto>> getLocationList(LocationSearchDto searchDto) {
        return ApiResponse.ok(locationService.getLocationList(searchDto));
    }

    @GetMapping("/{locationId}")
    public ApiResponse<LocationDto> getLocation(@PathVariable("locationId") Long locationId) {
        return ApiResponse.ok(locationService.getLocation(locationId));
    }

    @PostMapping
    public ApiResponse<Long> createLocation(@RequestBody LocationDto locationDto) {
        LocationDto dtoWithUser = new LocationDto(
                null, locationDto.getZoneId(), locationDto.getLocationCd(), locationDto.getLocationNm(),
                locationDto.getMaxCapacity(), 0.0, "EMPTY", locationDto.getUseYn(),
                "SYSTEM", null, null, null, null, null, null, null, null, null, null
        );
        return ApiResponse.ok(locationService.createLocation(dtoWithUser));
    }

    @PutMapping("/{locationId}")
    public ApiResponse<Void> updateLocation(@PathVariable("locationId") Long locationId, @RequestBody LocationDto locationDto) {
        LocationDto dtoWithId = new LocationDto(
                locationId, null, null, locationDto.getLocationNm(),
                locationDto.getMaxCapacity(), null, locationDto.getLocationStatus(), locationDto.getUseYn(),
                null, null, "SYSTEM", null, null, null, null, null, null, null, null
        );
        locationService.updateLocation(dtoWithId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{locationId}")
    public ApiResponse<Void> deleteLocation(@PathVariable("locationId") Long locationId) {
        locationService.deleteLocation(locationId);
        return ApiResponse.ok();
    }
}
