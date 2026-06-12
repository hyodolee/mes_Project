package com.mes.interfaces.api.ai;

import com.mes.application.service.ai.analysis.OperationAiAnalysisService;
import com.mes.domain.ai.dto.GlobalOperationAiAnalysisResponse;
import com.mes.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/operations")
public class OperationAiAnalysisApiController {

    private final OperationAiAnalysisService operationAiAnalysisService;

    public OperationAiAnalysisApiController(OperationAiAnalysisService operationAiAnalysisService) {
        this.operationAiAnalysisService = operationAiAnalysisService;
    }

    @GetMapping("/summary")
    public ApiResponse<GlobalOperationAiAnalysisResponse> getSummary(
            @RequestParam(name = "refresh", defaultValue = "false") boolean refresh
    ) {
        return ApiResponse.ok(operationAiAnalysisService.analyze(refresh));
    }
}
