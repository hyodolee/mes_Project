package com.mes.domain.ai.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GlobalOperationAiAnalysisResponse {

    private String severity;         // NORMAL, WARNING, CRITICAL
    private String summary;          // 전체 상황 요약 (여러 문장)
    private List<String> keyIssues;  // 주요 이슈 리스트
    private String inference;        // 전체적인 병목 및 원인 추정
    private String productionImpact; // 생산에 미치는 영향
    private List<String> recommendedActions; // 우선순위 권장 조치
    private List<AreaAssessmentDto> areaAssessments; // 영역별 AI 한 줄 진단
    private GlobalOperationEvidenceDto evidence; // 분석 근거 데이터
    private boolean aiGenerated;
    private String model;
}
