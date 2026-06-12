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
public class NaturalLanguageQueryResponse {
    private String answer;
    private String queryType;       // WORK_ORDER / TRANSFER / PLC_EVENT / INVENTORY / GENERAL
    private List<String> dataPoints; // 근거로 사용한 데이터 요약
    private boolean aiGenerated;
    private String model;
}
