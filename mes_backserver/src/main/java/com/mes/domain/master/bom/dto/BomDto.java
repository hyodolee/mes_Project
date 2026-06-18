package com.mes.domain.master.bom.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BOM(자재명세서) 1행. 모품목 → 자품목 + 소요량 관계.
 * 목록 조회 시 MST_ITEM과 조인해 품목명을 함께 내려준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BomDto {
    private Long bomId;
    private String plantCd;
    private String parentItemCd;
    private String parentItemNm;
    private String childItemCd;
    private String childItemNm;
    private Integer bomLevel;
    private Double bomQty;
    private Double lossRate;
    private String startDt;
    private String endDt;
    private String processCd;
    private String bomRmk;
    private String useYn;
}
