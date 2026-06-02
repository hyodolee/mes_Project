package com.mcs.interfaces.api.route;

import com.mcs.application.service.route.RouteService;
import com.mcs.domain.route.dto.RouteEdgeDto;
import com.mcs.domain.route.dto.RouteNodeDto;
import com.mcs.domain.route.dto.RouteOptimizeRequest;
import com.mcs.domain.route.dto.RouteOptimizeResultDto;
import com.mcs.domain.route.dto.RouteSearchDto;
import com.mcs.domain.route.dto.TransferRouteDto;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RouteApiController {

    private final RouteService routeService;

    @GetMapping("/route-nodes")
    public ApiResponse<List<RouteNodeDto>> getRouteNodes(RouteSearchDto searchDto) {
        return ApiResponse.ok(routeService.getRouteNodes(searchDto));
    }

    @GetMapping("/route-edges")
    public ApiResponse<List<RouteEdgeDto>> getRouteEdges(RouteSearchDto searchDto) {
        return ApiResponse.ok(routeService.getRouteEdges(searchDto));
    }

    @PatchMapping("/route-edges/{routeEdgeId}/status")
    public ApiResponse<Void> changeEdgeStatus(@PathVariable Long routeEdgeId, @RequestParam String status) {
        routeService.changeEdgeStatus(routeEdgeId, status, "SYSTEM");
        return ApiResponse.ok();
    }

    @PostMapping("/routes/optimize")
    public ApiResponse<RouteOptimizeResultDto> optimize(@RequestBody RouteOptimizeRequest request) {
        return ApiResponse.ok(routeService.optimize(request));
    }

    @GetMapping("/transfers/{transferId}/routes")
    public ApiResponse<TransferRouteDto> getTransferRoute(@PathVariable Long transferId) {
        return ApiResponse.ok(routeService.getTransferRoute(transferId).orElse(null));
    }

    @PostMapping("/transfers/{transferId}/routes")
    public ApiResponse<TransferRouteDto> createTransferRoute(
            @PathVariable Long transferId,
            @RequestParam(defaultValue = "SHORTEST_TIME") String optimizeRule
    ) {
        return ApiResponse.ok(routeService.createRouteForTransfer(transferId, optimizeRule, "SYSTEM"));
    }

    @PostMapping("/transfers/{transferId}/routes/replan")
    public ApiResponse<TransferRouteDto> replanTransferRoute(
            @PathVariable Long transferId,
            @RequestParam(defaultValue = "AVOID_CONGESTION") String optimizeRule
    ) {
        return ApiResponse.ok(routeService.createRouteForTransfer(transferId, optimizeRule, "SYSTEM"));
    }
}
