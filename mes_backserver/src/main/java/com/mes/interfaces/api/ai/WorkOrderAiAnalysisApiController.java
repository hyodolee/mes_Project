package com.mes.interfaces.api.ai;

import com.mes.application.service.ai.WorkOrderAiAnalysisService;
import com.mes.domain.ai.dto.WorkOrderAiAnalysisResponse;
import com.mes.global.response.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/work-orders")
public class WorkOrderAiAnalysisApiController {

    private final WorkOrderAiAnalysisService workOrderAiAnalysisService;

    public WorkOrderAiAnalysisApiController(WorkOrderAiAnalysisService workOrderAiAnalysisService) {
        this.workOrderAiAnalysisService = workOrderAiAnalysisService;
    }

    @PostMapping("/{woId}/analysis")
    public ApiResponse<WorkOrderAiAnalysisResponse> analyze(@PathVariable Long woId) {
        return ApiResponse.ok(workOrderAiAnalysisService.analyze(woId));
    }
}
