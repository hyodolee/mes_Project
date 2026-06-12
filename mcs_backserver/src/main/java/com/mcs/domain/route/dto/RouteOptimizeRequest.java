package com.mcs.domain.route.dto;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class RouteOptimizeRequest {
    private String plantCd;
    private Long fromLocationId;
    private Long toLocationId;
    private String optimizeRule;
}

