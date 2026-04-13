package com.mcs.global.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;

    public PageResponse(List<T> content, long totalElements, int totalPages, int currentPage, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
        this.size = size;
    }

    public static <T> PageResponse<T> createPagedResponse(List<T> content, long totalCount, PageRequest pageRequest) {
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
