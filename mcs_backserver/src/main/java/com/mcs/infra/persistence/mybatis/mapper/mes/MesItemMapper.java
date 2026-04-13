package com.mcs.infra.persistence.mybatis.mapper.mes;

import com.mcs.domain.mes.dto.ItemDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MesItemMapper {
    List<ItemDto> selectItemList(
        @Param("plantCd") String plantCd,
        @Param("itemCd") String itemCd,
        @Param("itemNm") String itemNm,
        @Param("itemType") String itemType
    );
}
