package com.mes.mcs.interfaces.api.inventory;

import com.mes.mcs.application.service.inventory.InventoryService;
import com.mes.mcs.domain.inventory.dto.LocStockDto;
import com.mes.mcs.domain.inventory.dto.LocStockSearchDto;
import com.mes.mcs.domain.inventory.dto.LocTransHisDto;
import com.mes.mcs.domain.inventory.dto.LocTransHisSearchDto;
import com.mes.mcs.domain.inventory.dto.StockAdjustRequest;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("mcsInventoryApiController")
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryApiController {

    private final InventoryService inventoryService;

    @GetMapping("/stocks")
    public ApiResponse<PageResponse<LocStockDto>> getLocStockList(LocStockSearchDto searchDto) {
        if (searchDto.getExcludeZeroStock() == null) {
            searchDto.setExcludeZeroStock(true);
        }
        return ApiResponse.ok(inventoryService.getLocStockList(searchDto));
    }

    @GetMapping("/stocks/{locStockId}")
    public ApiResponse<LocStockDto> getLocStock(@PathVariable Long locStockId) {
        return ApiResponse.ok(inventoryService.getLocStock(locStockId));
    }

    @PostMapping("/stocks/{locStockId}/adjust")
    public ApiResponse<Void> adjustStock(@PathVariable Long locStockId, @RequestBody StockAdjustRequest request) {
        double adjustQty = request.getAdjustQty() == null ? 0 : request.getAdjustQty();
        if ("ADJ_MINUS".equals(request.getAdjustType())) {
            adjustQty = -Math.abs(adjustQty);
        } else {
            adjustQty = Math.abs(adjustQty);
        }

        inventoryService.adjustStock(new StockAdjustRequest(
                locStockId,
                request.getAdjustType(),
                adjustQty,
                request.getTransRmk(),
                "SYSTEM"
        ));
        return ApiResponse.ok();
    }

    @GetMapping("/transactions")
    public ApiResponse<PageResponse<LocTransHisDto>> getLocTransHisList(LocTransHisSearchDto searchDto) {
        return ApiResponse.ok(inventoryService.getLocTransHisList(searchDto));
    }
}

