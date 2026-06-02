package com.mcs.interfaces.api.plc;

import com.mcs.application.service.plc.PlcEventService;
import com.mcs.domain.plc.dto.PlcEventDto;
import com.mcs.domain.plc.dto.PlcEventRequest;
import com.mcs.domain.plc.dto.PlcEventSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plc/events")
@RequiredArgsConstructor
public class PlcEventApiController {

    private final PlcEventService plcEventService;

    @GetMapping
    public ApiResponse<PageResponse<PlcEventDto>> getEvents(PlcEventSearchDto searchDto) {
        return ApiResponse.ok(plcEventService.getEventList(searchDto));
    }

    @PostMapping
    public ApiResponse<PlcEventDto> receiveEvent(@RequestBody PlcEventRequest request) {
        return ApiResponse.ok(plcEventService.receiveEvent(request));
    }
}
