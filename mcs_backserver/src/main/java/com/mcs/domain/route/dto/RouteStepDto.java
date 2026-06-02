package com.mcs.domain.route.dto;

public record RouteStepDto(
        Long routeStepId,
        Long transferRouteId,
        Integer stepSeq,
        Long routeEdgeId,
        Long fromNodeId,
        Long toNodeId,
        String stepStatus,
        Integer expectedTimeSec,
        String edgeCd,
        String edgeNm,
        String fromNodeCd,
        String fromNodeNm,
        String toNodeCd,
        String toNodeNm,
        String edgeStatus
) {}
