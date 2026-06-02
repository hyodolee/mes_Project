package com.mcs.domain.route.dto;

import java.time.LocalDateTime;

public record RouteNodeDto(
        Long routeNodeId,
        String plantCd,
        String nodeCd,
        String nodeNm,
        String nodeType,
        Long locationId,
        String useYn,
        String regUserId,
        LocalDateTime regDtm,
        String updUserId,
        LocalDateTime updDtm,
        String plantNm,
        String locationCd,
        String locationNm,
        String nodeTypeNm
) {}
