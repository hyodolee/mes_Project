package com.mes.interfaces.api.production;

import com.mes.application.service.production.WorkResultService;
import com.mes.domain.production.workresult.dto.WorkResultCreateRequest;
import com.mes.domain.production.workresult.dto.WorkResultDto;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/production/work-results")
public class WorkResultApiController {

    private final WorkResultService workResultService;

    public WorkResultApiController(WorkResultService workResultService) {
        this.workResultService = workResultService;
    }

    @GetMapping
    public ApiResponse<List<WorkResultDto>> getWorkResults(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "resultFromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate resultFromDt,
            @RequestParam(name = "resultToDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate resultToDt
    ) {
        return ApiResponse.ok(workResultService.getWorkResults(plantCd, itemCd, resultFromDt, resultToDt));
    }

    @PostMapping
    public ApiResponse<Void> createWorkResult(@Valid @RequestBody WorkResultCreateRequest request) {
        workResultService.createWorkResult(request);
        return ApiResponse.ok(null);
    }
}
