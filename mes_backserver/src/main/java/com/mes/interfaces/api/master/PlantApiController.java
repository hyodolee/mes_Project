package com.mes.interfaces.api.master;

import com.mes.application.service.master.PlantService;
import com.mes.domain.master.plant.dto.PlantDto;
import com.mes.domain.master.plant.dto.PlantUpsertRequest;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master/plants")
public class PlantApiController {

    private final PlantService plantService;

    public PlantApiController(PlantService plantService) {
        this.plantService = plantService;
    }

    @GetMapping
    public ApiResponse<List<PlantDto>> getPlants(
            @RequestParam(name = "companyCd", required = false) String companyCd,
            @RequestParam(name = "plantNm", required = false) String plantNm,
            @RequestParam(name = "useYn", required = false) String useYn
    ) {
        return ApiResponse.ok(plantService.getPlants(companyCd, plantNm, useYn));
    }

    @GetMapping("/{plantCd}")
    public ApiResponse<PlantDto> getPlant(@PathVariable("plantCd") String plantCd) {
        return ApiResponse.ok(plantService.getPlant(plantCd));
    }

    @PostMapping
    public ApiResponse<Void> createPlant(@Valid @RequestBody PlantUpsertRequest request) {
        plantService.createPlant(request);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{plantCd}")
    public ApiResponse<Void> updatePlant(@PathVariable("plantCd") String plantCd, @Valid @RequestBody PlantUpsertRequest request) {
        if (!plantCd.equals(request.plantCd())) {
            throw new IllegalArgumentException("경로의 plantCd와 요청 본문의 plantCd가 일치하지 않습니다.");
        }
        plantService.updatePlant(request);
        return ApiResponse.ok(null);
    }
}
