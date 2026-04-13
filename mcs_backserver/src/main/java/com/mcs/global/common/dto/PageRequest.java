package com.mcs.global.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRequest {
    private int page = 1;
    private int size = 10;

    public int getOffset() {
        // page가 0(첫 페이지)으로 올 경우를 대비해 0보다 작아지지 않게 처리
        int p = (page < 1) ? 0 : page - 1;
        return p * size;
    }

    public int getPage() { return page; }
    public int getSize() { return size; }
}
