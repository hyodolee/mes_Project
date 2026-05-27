package com.mcs.interfaces.api.plc;

import com.mcs.application.service.plc.PlcEventService;
import com.mcs.domain.plc.dto.PlcEventDto;
import com.mcs.domain.plc.dto.PlcEventRequest;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plc/events")
@RequiredArgsConstructor
public class PlcEventApiController {

    private final PlcEventService plcEventService;

    @PostMapping
    public ApiResponse<PlcEventDto> receiveEvent(@RequestBody PlcEventRequest request) {
        return ApiResponse.ok(plcEventService.receiveEvent(request));
    }
}
