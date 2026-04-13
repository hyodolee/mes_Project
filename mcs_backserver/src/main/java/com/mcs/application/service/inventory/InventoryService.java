package com.mcs.application.service.inventory;

import com.mcs.domain.inventory.dto.*;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryMapper inventoryMapper;

    public PageResponse<LocStockDto> getLocStockList(LocStockSearchDto searchDto) {
        List<LocStockDto> list = inventoryMapper.selectLocStockList(searchDto);
        long total = inventoryMapper.selectLocStockCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public LocStockDto getLocStock(Long locStockId) {
        return inventoryMapper.selectLocStockById(locStockId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT)); // TODO: Add specific error code if needed
    }

    public PageResponse<LocTransHisDto> getLocTransHisList(LocTransHisSearchDto searchDto) {
        List<LocTransHisDto> list = inventoryMapper.selectLocTransHisList(searchDto);
        long total = inventoryMapper.selectLocTransHisCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    @Transactional
    public void adjustStock(StockAdjustRequest request) {
        LocStockDto currentStock = getLocStock(request.locStockId());

        double adjustQty = request.adjustQty();
        double afterQty = currentStock.stockQty() + adjustQty;

        if (afterQty < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT); // 재고는 0보다 작을 수 없음
        }

        // 1. 재고 업데이트
        inventoryMapper.updateLocStockQty(request.locStockId(), adjustQty, request.regUserId());

        // 2. 이력 등록
        LocTransHisDto hisDto = new LocTransHisDto(
            null,
            currentStock.plantCd(),
            currentStock.locStockId(),
            request.adjustType(),
            Math.abs(adjustQty), // 이력에는 양수로 저장하고 타입으로 구분
            currentStock.stockQty(),
            afterQty,
            "ADJ",
            null,
            null,
            request.transRmk(),
            request.regUserId(),
            null,
            null, null, null, null, null, null, null, null
        );
        inventoryMapper.insertLocTransHis(hisDto);
    }
}
