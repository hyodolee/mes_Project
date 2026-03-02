package com.mes.interfaces.api.planning;

import com.mes.application.service.planning.ProdPlanService;
import com.mes.domain.planning.prodplan.dto.ProdPlanCreateRequest;
import com.mes.domain.planning.prodplan.dto.ProdPlanDto;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/planning/prod-plans")
public class ProdPlanApiController {

    private final ProdPlanService prodPlanService;

    public ProdPlanApiController(ProdPlanService prodPlanService) {
        this.prodPlanService = prodPlanService;
    }

    @GetMapping
    public ApiResponse<List<ProdPlanDto>> getProdPlans(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "planStatus", required = false) String planStatus,
            @RequestParam(name = "planFromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planFromDt,
            @RequestParam(name = "planToDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planToDt
    ) {
        return ApiResponse.ok(prodPlanService.getProdPlans(plantCd, itemCd, planStatus, planFromDt, planToDt));
    }

    @GetMapping("/{planId}")
    public ApiResponse<ProdPlanDto> getProdPlan(@PathVariable("planId") Long planId) {
        return ApiResponse.ok(prodPlanService.getProdPlan(planId));
    }

    @PostMapping
    public ApiResponse<Void> createProdPlan(@Valid @RequestBody ProdPlanCreateRequest request) {
        prodPlanService.createProdPlan(request);
        return ApiResponse.ok(null);
    }
}
