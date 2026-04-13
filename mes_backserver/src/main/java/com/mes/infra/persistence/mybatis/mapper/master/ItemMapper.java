package com.mes.infra.persistence.mybatis.mapper.master;
import com.mes.domain.master.item.dto.ItemDto;
import com.mes.domain.master.item.dto.ItemSearchDto;
import com.mes.domain.master.item.dto.ItemUpsertRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItemMapper {
    List<ItemDto> findItems(@Param("itemNm") String itemNm, @Param("useYn") String useYn);

    List<ItemDto> selectItemList(ItemSearchDto searchDto);

    int countItems(ItemSearchDto searchDto);

    ItemDto getItem(String itemCd);

    void save(ItemUpsertRequest request);

    void update(ItemUpsertRequest request);
}
