package com.mes.interfaces.web.inventory;

import com.mes.application.service.inventory.InventoryService;
import com.mes.domain.inventory.stock.dto.StockDto;
import com.mes.domain.inventory.stock.dto.StockSearchDto;
import com.mes.global.common.dto.PageResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inventory/stocks")
public class StockWebController {

    private final InventoryService inventoryService;

    public StockWebController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public String list(@ModelAttribute StockSearchDto searchDto, Model model) {
        
        PageResponse<StockDto> pageResponse = inventoryService.getStockPage(searchDto);
        model.addAttribute("stocks", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("active", "stocks");
        return "inventory/stock/list";
    }
}
