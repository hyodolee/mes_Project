package com.mes.infra.persistence.mybatis.mapper.master;

import com.mes.domain.master.item.dto.ItemDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ItemMapper {
    List<ItemDto> findItems(@Param("itemNm") String itemNm, @Param("useYn") String useYn);
}
