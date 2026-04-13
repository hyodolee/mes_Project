package com.mes.global.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 페이징 처리된 결과를 담는 공통 응답 클래스
 */
@Getter
@Setter
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content;        // 실제 데이터 목록
    private int totalElements;      // 전체 데이터 개수
    private int totalPages;         // 전체 페이지 수
    private int currentPage;        // 현재 페이지 번호
    private int size;               // 한 페이지당 데이터 개수

    public PageResponse(List<T> content, int totalElements, int totalPages, int currentPage, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.size = size;
    }

    /**
     * 조회된 리스트와 요청 정보를 바탕으로 페이징 응답 객체를 생성합니다.
     */
    public static <T> PageResponse<T> createPagedResponse(List<T> content, int totalCount, PageRequest pageRequest) {
        // 전체 페이지 수 계산
        int totalPages = (int) Math.ceil((double) totalCount / pageRequest.getSize());
        
        return new PageResponse<>(
                content,
                totalCount,
                totalPages,
                pageRequest.getPage(),
                pageRequest.getSize()
        );
    }
}
