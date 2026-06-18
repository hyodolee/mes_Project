package com.mes.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 영역별 AI 한 줄 진단 (생산/이송/품질/재고/설비).
 * 카드에 숫자와 함께 표시해 AI가 영역마다 무엇이 문제인지 짚어준다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AreaAssessmentDto {
    private String area;    // 생산/이송/품질/재고/설비
    private String status;  // NORMAL/WARNING/CRITICAL
    private String comment; // 한 줄 진단
}
