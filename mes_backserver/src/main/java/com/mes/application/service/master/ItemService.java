package com.mes.application.service.master;

import com.mes.domain.master.item.dto.ItemDto;
import com.mes.domain.master.item.dto.ItemSearchDto;
import com.mes.domain.master.item.dto.ItemUpsertRequest;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
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

    public PageResponse<ItemDto> getItemList(ItemSearchDto searchDto) {
        List<ItemDto> items = itemMapper.selectItemList(searchDto);
        int totalCount = itemMapper.countItems(searchDto);
        
        return PageResponse.createPagedResponse(items, totalCount, searchDto);
    }

    public ItemDto getItem(String itemCd) {
        ItemDto item = itemMapper.getItem(itemCd);
        if (item == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "해당 품목을 찾을 수 없습니다. 코드: " + itemCd);
        }
        return item;
    }

    @Transactional
    public void createItem(ItemUpsertRequest request) {
        if (itemMapper.getItem(request.itemCd()) != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 존재하는 품목 코드입니다.");
        }
        itemMapper.save(request);
    }

    @Transactional
    public void updateItem(ItemUpsertRequest request) {
        getItem(request.itemCd()); // 존재 여부 확인
        itemMapper.update(request);
    }
}
