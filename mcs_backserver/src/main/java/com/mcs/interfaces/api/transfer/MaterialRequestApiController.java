package com.mcs.interfaces.api.transfer;

import com.mcs.application.service.transfer.MaterialRequestService;
import com.mcs.domain.transfer.dto.MaterialRequestDto;
import com.mcs.domain.transfer.dto.MaterialRequestResultDto;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/material-requests")
@RequiredArgsConstructor
public class MaterialRequestApiController {

    private final MaterialRequestService materialRequestService;

    @PostMapping
    public ApiResponse<MaterialRequestResultDto> createMaterialRequest(@RequestBody MaterialRequestDto request) {
        return ApiResponse.ok(materialRequestService.createMaterialRequest(request));
    }

    @PostMapping("/work-orders/{woId}/cancel")
    public ApiResponse<Integer> cancelMaterialRequestsByWorkOrder(@PathVariable Long woId) {
        return ApiResponse.ok(materialRequestService.cancelMaterialRequestsByWorkOrder(woId));
    }
}
