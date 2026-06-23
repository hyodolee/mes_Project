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
public class NaturalLanguageQueryRequest {

    private String question;
    private String conversationId; // 서버 측 ChatMemory conversation id
    private String pageContext; // 현재 페이지 경로 (예: /mcs/transfers)
    private List<ChatTurnDto> history; // 직전 대화 (후속 질문 맥락용, 최대 6턴)

    // 이번 질문에 새로 첨부한 이미지들 (base64 data URL). 보관·재첨부는 서버가 담당.
    private List<String> images;
}
