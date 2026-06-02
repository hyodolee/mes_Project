package com.mcs.domain.route.dto;

import java.util.List;

public record RouteOptimizeResultDto(
        Boolean routeAvailable,
        String message,
        String optimizeRule,
        Double totalDistanceM,
        Integer totalTimeSec,
        Double totalCost,
        List<RouteStepDto> steps
) {}
