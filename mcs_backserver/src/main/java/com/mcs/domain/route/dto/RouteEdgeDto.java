package com.mcs.domain.route.dto;

import java.time.LocalDateTime;

public record RouteEdgeDto(
        Long routeEdgeId,
        String plantCd,
        String edgeCd,
        String edgeNm,
        Long fromNodeId,
        Long toNodeId,
        String bidirectionalYn,
        Double distanceM,
        Integer travelTimeSec,
        Double baseCost,
        String edgeStatus,
        String useYn,
        String regUserId,
        LocalDateTime regDtm,
        String updUserId,
        LocalDateTime updDtm,
        String fromNodeCd,
        String fromNodeNm,
        String toNodeCd,
        String toNodeNm,
        String edgeStatusNm
) {}
