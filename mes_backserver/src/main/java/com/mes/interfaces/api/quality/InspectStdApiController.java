package com.mes.interfaces.api.quality;

import com.mes.application.service.quality.InspectStdService;
import com.mes.domain.quality.inspectstd.dto.InspectStdDto;
import com.mes.global.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/quality/inspect-stds")
public class InspectStdApiController {

    private final InspectStdService inspectStdService;

    public InspectStdApiController(InspectStdService inspectStdService) {
        this.inspectStdService = inspectStdService;
    }

    @GetMapping
    public ApiResponse<List<InspectStdDto>> getInspectStds(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "inspectType", required = false) String inspectType
    ) {
        return ApiResponse.ok(inspectStdService.getInspectStds(plantCd, itemCd, inspectType));
    }
}
