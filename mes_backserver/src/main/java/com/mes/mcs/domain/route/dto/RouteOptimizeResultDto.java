package com.mes.mcs.domain.route.dto;

import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class RouteOptimizeResultDto {
    private Boolean routeAvailable;
    private String message;
    private String optimizeRule;
    private Double totalDistanceM;
    private Integer totalTimeSec;
    private Double totalCost;
    private List<RouteStepDto> steps;
}
