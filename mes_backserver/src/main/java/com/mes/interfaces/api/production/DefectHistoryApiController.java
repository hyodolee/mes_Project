package com.mes.interfaces.api.production;

import com.mes.application.service.production.DefectHistoryService;
import com.mes.domain.production.defect.dto.DefectHistoryCreateRequest;
import com.mes.domain.production.defect.dto.DefectHistoryDto;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/production/defect-histories")
public class DefectHistoryApiController {

    private final DefectHistoryService defectHistoryService;

    public DefectHistoryApiController(DefectHistoryService defectHistoryService) {
        this.defectHistoryService = defectHistoryService;
    }

    @GetMapping
    public ApiResponse<List<DefectHistoryDto>> getDefectHistories(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "fromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDt,
            @RequestParam(name = "toDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDt
    ) {
        return ApiResponse.ok(defectHistoryService.getDefectHistories(plantCd, itemCd, fromDt, toDt));
    }

    @PostMapping
    public ApiResponse<Void> createDefectHistory(@Valid @RequestBody DefectHistoryCreateRequest request) {
        defectHistoryService.createDefectHistory(request);
        return ApiResponse.ok(null);
    }
}
