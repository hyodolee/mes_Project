package com.mes.application.service.master;

import com.mes.domain.master.item.dto.ItemDto;
import com.mes.infra.persistence.mybatis.mapper.master.ItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemMapper itemMapper;

    public ItemService(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    public List<ItemDto> getItems(String itemNm, String useYn) {
        return itemMapper.findItems(itemNm, useYn);
    }
}
