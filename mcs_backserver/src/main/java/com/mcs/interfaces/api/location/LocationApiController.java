package com.mcs.interfaces.api.location;

import com.mcs.application.service.location.LocationService;
import com.mcs.domain.location.dto.LocationDto;
import com.mcs.domain.location.dto.LocationSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationApiController {

    private final LocationService locationService;

    @GetMapping
    public ApiResponse<PageResponse<LocationDto>> getLocationList(LocationSearchDto searchDto) {
        return ApiResponse.ok(locationService.getLocationList(searchDto));
    }

    @GetMapping("/{locationId}")
    public ApiResponse<LocationDto> getLocation(@PathVariable Long locationId) {
        return ApiResponse.ok(locationService.getLocation(locationId));
    }

    @PostMapping
    public ApiResponse<Long> createLocation(@RequestBody LocationDto locationDto) {
        LocationDto dtoWithUser = new LocationDto(
            null, locationDto.zoneId(), locationDto.locationCd(), locationDto.locationNm(),
            locationDto.maxCapacity(), 0.0, "EMPTY", locationDto.useYn(),
            "SYSTEM", null, null, null, null, null, null, null, null, null, null
        );
        return ApiResponse.ok(locationService.createLocation(dtoWithUser));
    }

    @PutMapping("/{locationId}")
    public ApiResponse<Void> updateLocation(@PathVariable Long locationId, @RequestBody LocationDto locationDto) {
        LocationDto dtoWithId = new LocationDto(
            locationId, null, null, locationDto.locationNm(),
            locationDto.maxCapacity(), null, locationDto.locationStatus(), locationDto.useYn(),
            null, null, "SYSTEM", null, null, null, null, null, null, null, null
        );
        locationService.updateLocation(dtoWithId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{locationId}")
    public ApiResponse<Void> deleteLocation(@PathVariable Long locationId) {
        locationService.deleteLocation(locationId);
        return ApiResponse.ok();
    }
}
