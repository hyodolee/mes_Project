package com.mcs.domain.route.dto;

public record RouteOptimizeRequest(
        String plantCd,
        Long fromLocationId,
        Long toLocationId,
        String optimizeRule
) {}
