package com.mes.mcs.domain.route.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteSearchDto {
    private String plantCd;
    private String nodeCd;
    private String edgeCd;
    private String edgeStatus;
}
