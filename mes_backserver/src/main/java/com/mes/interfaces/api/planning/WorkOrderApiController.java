package com.mes.interfaces.api.planning;

import com.mes.application.service.planning.WorkOrderService;
import com.mes.domain.planning.workorder.dto.WorkOrderCreateRequest;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import com.mes.domain.planning.workorder.dto.WorkOrderUpdateStatusRequest;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/planning/work-orders")
public class WorkOrderApiController {

    private final WorkOrderService workOrderService;

    public WorkOrderApiController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public ApiResponse<List<WorkOrderDto>> getWorkOrders(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "woStatus", required = false) String woStatus,
            @RequestParam(name = "woFromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate woFromDt,
            @RequestParam(name = "woToDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate woToDt
    ) {
        return ApiResponse.ok(workOrderService.getWorkOrders(plantCd, itemCd, woStatus, woFromDt, woToDt));
    }

    @GetMapping("/{woId}")
    public ApiResponse<WorkOrderDto> getWorkOrder(@PathVariable("woId") Long woId) {
        return ApiResponse.ok(workOrderService.getWorkOrder(woId));
    }

    @PostMapping
    public ApiResponse<Void> createWorkOrder(@Valid @RequestBody WorkOrderCreateRequest request) {
        workOrderService.createWorkOrder(request);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{woId}/status")
    public ApiResponse<Void> updateWorkOrderStatus(@PathVariable("woId") Long woId,
                                                   @Valid @RequestBody WorkOrderUpdateStatusRequest request) {
        workOrderService.updateWorkOrderStatus(woId, request.woStatus());
        return ApiResponse.ok(null);
    }
}
