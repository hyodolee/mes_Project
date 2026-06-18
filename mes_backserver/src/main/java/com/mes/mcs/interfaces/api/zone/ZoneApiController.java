package com.mes.mcs.interfaces.api.zone;

import com.mes.global.common.dto.PageResponse;
import com.mes.global.response.ApiResponse;
import com.mes.mcs.application.service.zone.ZoneService;
import com.mes.mcs.domain.zone.dto.ZoneDto;
import com.mes.mcs.domain.zone.dto.ZoneSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("mcsZoneApiController")
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ZoneApiController {

    private final ZoneService zoneService;

    @GetMapping
    public ApiResponse<PageResponse<ZoneDto>> getZoneList(ZoneSearchDto searchDto) {
        return ApiResponse.ok(zoneService.getZoneList(searchDto));
    }

    @GetMapping("/{zoneId}")
    public ApiResponse<ZoneDto> getZone(@PathVariable("zoneId") Long zoneId) {
        return ApiResponse.ok(zoneService.getZone(zoneId));
    }

    @PostMapping
    public ApiResponse<Long> createZone(@RequestBody ZoneDto zoneDto) {
        ZoneDto dtoWithUser = new ZoneDto(
                null, zoneDto.getPlantCd(), zoneDto.getWarehouseCd(), zoneDto.getZoneCd(),
                zoneDto.getZoneNm(), zoneDto.getZoneType(), zoneDto.getSortSeq(), zoneDto.getUseYn(),
                "SYSTEM", null, null, null, null, null, null
        );
        return ApiResponse.ok(zoneService.createZone(dtoWithUser));
    }

    @PutMapping("/{zoneId}")
    public ApiResponse<Void> updateZone(@PathVariable("zoneId") Long zoneId, @RequestBody ZoneDto zoneDto) {
        ZoneDto dtoWithId = new ZoneDto(
                zoneId, null, null, null,
                zoneDto.getZoneNm(), zoneDto.getZoneType(), zoneDto.getSortSeq(), zoneDto.getUseYn(),
                null, null, "SYSTEM", null, null, null, null
        );
        zoneService.updateZone(dtoWithId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{zoneId}")
    public ApiResponse<Void> deleteZone(@PathVariable("zoneId") Long zoneId) {
        zoneService.deleteZone(zoneId);
        return ApiResponse.ok();
    }
}
