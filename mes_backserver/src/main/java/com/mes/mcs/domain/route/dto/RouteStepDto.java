package com.mes.mcs.domain.route.dto;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class RouteStepDto {
    private Long routeStepId;
    private Long transferRouteId;
    private Integer stepSeq;
    private Long routeEdgeId;
    private Long fromNodeId;
    private Long toNodeId;
    private String stepStatus;
    private Integer expectedTimeSec;
    private String edgeCd;
    private String edgeNm;
    private String fromNodeCd;
    private String fromNodeNm;
    private String toNodeCd;
    private String toNodeNm;
    private String edgeStatus;
}
