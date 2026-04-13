package com.mes.interfaces.api.quality;

import com.mes.application.service.quality.InspectResultService;
import com.mes.domain.quality.inspectresult.dto.InspectResultCreateRequest;
import com.mes.domain.quality.inspectresult.dto.InspectResultDto;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/quality/inspect-results")
public class InspectResultApiController {

    private final InspectResultService inspectResultService;

    public InspectResultApiController(InspectResultService inspectResultService) {
        this.inspectResultService = inspectResultService;
    }

    @GetMapping
    public ApiResponse<List<InspectResultDto>> getInspectResults(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "fromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDt,
            @RequestParam(name = "toDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDt
    ) {
        return ApiResponse.ok(inspectResultService.getInspectResults(plantCd, itemCd, fromDt, toDt));
    }

    @GetMapping("/{inspectId}")
    public ApiResponse<InspectResultDto> getInspectResult(@PathVariable("inspectId") Long inspectId) {
        return ApiResponse.ok(inspectResultService.getInspectResult(inspectId));
    }

    @PostMapping
    public ApiResponse<Void> createInspectResult(@Valid @RequestBody InspectResultCreateRequest request) {
        inspectResultService.createInspectResult(request);
        return ApiResponse.ok(null);
    }
}
