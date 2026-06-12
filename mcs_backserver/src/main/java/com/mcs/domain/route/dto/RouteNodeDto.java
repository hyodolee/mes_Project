package com.mcs.domain.route.dto;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class RouteNodeDto {
    private Long routeNodeId;
    private String plantCd;
    private String nodeCd;
    private String nodeNm;
    private String nodeType;
    private Long locationId;
    private String useYn;
    private String regUserId;
    private LocalDateTime regDtm;
    private String updUserId;
    private LocalDateTime updDtm;
    private String plantNm;
    private String locationCd;
    private String locationNm;
    private String nodeTypeNm;
}

