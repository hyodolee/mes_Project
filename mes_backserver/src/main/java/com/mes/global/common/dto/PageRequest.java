package com.mes.global.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageRequest {
    private int page = 1;
    private int size = 10;

    public int getOffset() {
        return (page - 1) * size;
    }
}
