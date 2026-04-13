package com.mcs.domain.mes.dto;

public record ComCodeDto(
    String grpCd,
    String comCd,
    String comNm,
    Integer sortSeq,
    String attr1,
    String useYn
) {}
