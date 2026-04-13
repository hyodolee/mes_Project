package com.mes.interfaces.api.inventory;

import com.mes.application.service.inventory.InventoryService;
import com.mes.domain.inventory.stock.dto.StockDto;
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import com.mes.domain.inventory.trans.dto.TransDto;
import com.mes.domain.inventory.trans.dto.TransRequest;
import com.mes.global.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryApiController {

    private final InventoryService inventoryService;

    public InventoryApiController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/stocks")
    public ApiResponse<List<StockDto>> getStocks(@ModelAttribute StockSearchDto searchDto) {
        return ApiResponse.ok(inventoryService.getStocks(searchDto));
    }

    @GetMapping("/trans-histories")
    public ApiResponse<List<TransDto>> getTransHistories(
            @RequestParam(name = "plantCd", required = false) String plantCd,
            @RequestParam(name = "itemCd", required = false) String itemCd,
            @RequestParam(name = "fromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDt,
            @RequestParam(name = "toDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDt
    ) {
        return ApiResponse.ok(inventoryService.getTransHistories(plantCd, itemCd, fromDt, toDt));
    }

    @PostMapping("/trans")
    public ApiResponse<Void> processTransaction(@Valid @RequestBody TransRequest request) {
        inventoryService.processTransaction(request);
        return ApiResponse.ok(null);
    }
}
