package com.mcs.domain.route.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TransferRouteDto(
        Long transferRouteId,
        Long transferId,
        String routeStatus,
        Double totalDistanceM,
        Integer totalTimeSec,
        Double totalCost,
        String optimizeRule,
        Integer replanCount,
        String regUserId,
        LocalDateTime regDtm,
        String updUserId,
        LocalDateTime updDtm,
        List<RouteStepDto> steps
) {}
