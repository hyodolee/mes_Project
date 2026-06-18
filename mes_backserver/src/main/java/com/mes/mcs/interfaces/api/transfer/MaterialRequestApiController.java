package com.mes.mcs.interfaces.api.transfer;

import com.mes.mcs.application.service.transfer.MaterialRequestService;
import com.mes.mcs.domain.transfer.dto.MaterialRequestDto;
import com.mes.mcs.domain.transfer.dto.MaterialRequestResultDto;
import com.mes.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController("mcsMaterialRequestApiController")
@RequestMapping("/api/material-requests")
@RequiredArgsConstructor
public class MaterialRequestApiController {

    private final MaterialRequestService materialRequestService;

    @PostMapping
    public ApiResponse<MaterialRequestResultDto> createMaterialRequest(@RequestBody MaterialRequestDto request) {
        return ApiResponse.ok(materialRequestService.createMaterialRequest(request));
    }

    @PostMapping("/work-orders/{woId}/cancel")
    public ApiResponse<Integer> cancelMaterialRequestsByWorkOrder(@PathVariable("woId") Long woId) {
        return ApiResponse.ok(materialRequestService.cancelMaterialRequestsByWorkOrder(woId));
    }
}
