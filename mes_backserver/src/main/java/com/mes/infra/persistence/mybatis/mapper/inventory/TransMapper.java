package com.mes.infra.persistence.mybatis.mapper.inventory;

import com.mes.domain.inventory.trans.dto.TransDto;
import com.mes.domain.inventory.trans.dto.TransRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TransMapper {
    List<TransDto> selectTransHistories(
        @Param("plantCd") String plantCd,
        @Param("itemCd") String itemCd,
        @Param("fromDt") LocalDate fromDt,
        @Param("toDt") LocalDate toDt
    );

    int insertTransHistory(
        @Param("request") TransRequest request,
        @Param("transNo") String transNo,
        @Param("transDt") LocalDate transDt
    );

    int selectTransCountByDate(@Param("transDt") LocalDate transDt);
}
