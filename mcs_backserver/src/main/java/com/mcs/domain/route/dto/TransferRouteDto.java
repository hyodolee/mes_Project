package com.mcs.domain.route.dto;

import java.time.LocalDateTime;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class TransferRouteDto {
    private Long transferRouteId;
    private Long transferId;
    private String routeStatus;
    private Double totalDistanceM;
    private Integer totalTimeSec;
    private Double totalCost;
    private String optimizeRule;
    private Integer replanCount;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    private List<RouteStepDto> steps;
}

