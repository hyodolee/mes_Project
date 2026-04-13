package com.mcs.interfaces.api.zone;

import com.mcs.application.service.zone.ZoneService;
import com.mcs.domain.zone.dto.ZoneDto;
import com.mcs.domain.zone.dto.ZoneSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/zones")
@RequiredArgsConstructor
public class ZoneApiController {

    private final ZoneService zoneService;

    @GetMapping
    public ApiResponse<PageResponse<ZoneDto>> getZoneList(ZoneSearchDto searchDto) {
        return ApiResponse.ok(zoneService.getZoneList(searchDto));
    }

    @GetMapping("/{zoneId}")
    public ApiResponse<ZoneDto> getZone(@PathVariable Long zoneId) {
        return ApiResponse.ok(zoneService.getZone(zoneId));
    }

    @PostMapping
    public ApiResponse<Long> createZone(@RequestBody ZoneDto zoneDto) {
        // 실제 운영 시 세션 등에서 사용자 ID 추출
        ZoneDto dtoWithUser = new ZoneDto(
            null, zoneDto.plantCd(), zoneDto.warehouseCd(), zoneDto.zoneCd(),
            zoneDto.zoneNm(), zoneDto.zoneType(), zoneDto.sortSeq(), zoneDto.useYn(),
            "SYSTEM", null, null, null, null, null, null
        );
        return ApiResponse.ok(zoneService.createZone(dtoWithUser));
    }

    @PutMapping("/{zoneId}")
    public ApiResponse<Void> updateZone(@PathVariable Long zoneId, @RequestBody ZoneDto zoneDto) {
        ZoneDto dtoWithId = new ZoneDto(
            zoneId, null, null, null,
            zoneDto.zoneNm(), zoneDto.zoneType(), zoneDto.sortSeq(), zoneDto.useYn(),
            null, null, "SYSTEM", null, null, null, null
        );
        zoneService.updateZone(dtoWithId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{zoneId}")
    public ApiResponse<Void> deleteZone(@PathVariable Long zoneId) {
        zoneService.deleteZone(zoneId);
        return ApiResponse.ok();
    }
}
