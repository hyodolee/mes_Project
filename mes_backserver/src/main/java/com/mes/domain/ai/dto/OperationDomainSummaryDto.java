package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 운영 브리핑 AI에 추가로 넘기는 영역별 요약(품질·재고·설비).
 * 작업·이송 외에 공장 전체를 종합 판단하도록 재료를 보강한다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperationDomainSummaryDto {
    // 품질
    private long inspectTotal;
    private long inspectFailed;
    private long defectCount;
    // 재고
    private long stockLow;        // 부족
    private long stockRestricted; // 사용 제한
    // 설비
    private long equipRunning;
    private long equipDown;       // 비가동
    private long equipDowntime;   // 비가동 이력 건수
}
