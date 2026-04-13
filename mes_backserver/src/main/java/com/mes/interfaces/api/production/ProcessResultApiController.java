package com.mes.interfaces.api.production;

import com.mes.application.service.production.ProcessResultService;
import com.mes.domain.production.processresult.dto.ProcessResultCreateRequest;
import com.mes.domain.production.processresult.dto.ProcessResultDto;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/production/process-results")
public class ProcessResultApiController {

    private final ProcessResultService processResultService;

    public ProcessResultApiController(ProcessResultService processResultService) {
        this.processResultService = processResultService;
    }

    @GetMapping("/{resultId}")
    public ApiResponse<List<ProcessResultDto>> getProcessResults(@PathVariable("resultId") Long resultId) {
        return ApiResponse.ok(processResultService.getProcessResults(resultId));
    }

    @PostMapping
    public ApiResponse<Void> createProcessResult(@Valid @RequestBody ProcessResultCreateRequest request) {
        processResultService.createProcessResult(request);
        return ApiResponse.ok(null);
    }
}
