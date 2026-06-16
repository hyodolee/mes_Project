package com.mes.mcs.application.service.inventory;

import com.mes.mcs.domain.inventory.dto.*;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("mcsInventoryService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryMapper inventoryMapper;

    public PageResponse<LocStockDto> getLocStockList(LocStockSearchDto searchDto) {
        List<LocStockDto> list = inventoryMapper.selectLocStockList(searchDto);
        long total = inventoryMapper.selectLocStockCount(searchDto);
        return PageResponse.createPagedResponse(list, Math.toIntExact(total), searchDto);
    }

    public LocStockDto getLocStock(Long locStockId) {
        return inventoryMapper.selectLocStockById(locStockId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT)); // TODO: Add specific error code if needed
    }

    public PageResponse<LocTransHisDto> getLocTransHisList(LocTransHisSearchDto searchDto) {
        List<LocTransHisDto> list = inventoryMapper.selectLocTransHisList(searchDto);
        long total = inventoryMapper.selectLocTransHisCount(searchDto);
        return PageResponse.createPagedResponse(list, Math.toIntExact(total), searchDto);
    }

    public List<LocTransHisDto> getTransferHistories(String transferNo, Long transferId) {
        return inventoryMapper.selectLocTransHisByRef("TF", transferNo, transferId);
    }

    @Transactional
    public void adjustStock(StockAdjustRequest request) {
        LocStockDto currentStock = getLocStock(request.getLocStockId());

        double adjustQty = request.getAdjustQty();
        double afterQty = currentStock.getStockQty() + adjustQty;

        if (afterQty < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT); // 재고는 0보다 작을 수 없음
        }

        // 1. 재고 업데이트
        inventoryMapper.updateLocStockQty(request.getLocStockId(), adjustQty, request.getRegUserId());
        inventoryMapper.syncLocationUsage(currentStock.getLocationId(), request.getRegUserId());

        // 2. 이력 등록
        LocTransHisDto hisDto = new LocTransHisDto(
            null,
            currentStock.getPlantCd(),
            currentStock.getLocStockId(),
            request.getAdjustType(),
            Math.abs(adjustQty), // 이력에는 양수로 저장하고 타입으로 구분
            currentStock.getStockQty(),
            afterQty,
            "ADJ",
            null,
            null,
            request.getTransRmk(),
            request.getRegUserId(),
            null,
            null, null, null, null, null, null, null, null
        );
        inventoryMapper.insertLocTransHis(hisDto);
    }
}
