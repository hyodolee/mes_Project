package com.mcs.domain.route.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class RouteEdgeDto {
    private Long routeEdgeId;
    private String plantCd;
    private String edgeCd;
    private String edgeNm;
    private Long fromNodeId;
    private Long toNodeId;
    private String bidirectionalYn;
    private Double distanceM;
    private Integer travelTimeSec;
    private Double baseCost;
    private String edgeStatus;
    private String useYn;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    private String fromNodeCd;
    private String fromNodeNm;
    private String toNodeCd;
    private String toNodeNm;
    private String edgeStatusNm;
}

