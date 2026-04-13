package com.mcs.interfaces.web.inventory;

import com.mcs.application.service.inventory.InventoryService;
import com.mcs.application.service.zone.ZoneService;
import com.mcs.domain.inventory.dto.*;
import com.mcs.domain.zone.dto.ZoneSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesComCodeMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesPlantMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryWebController {

    private final InventoryService inventoryService;
    private final ZoneService zoneService;
    private final MesPlantMapper mesPlantMapper;
    private final MesWarehouseMapper mesWarehouseMapper;
    private final MesComCodeMapper mesComCodeMapper;

    // 1. 로케이션 재고 현황
    @GetMapping("/stock")
    public String stockList(LocStockSearchDto searchDto, Model model) {
        model.addAttribute("active", "stock");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        model.addAttribute("zones", zoneService.getZoneList(new ZoneSearchDto()).getContent());

        if (searchDto.getExcludeZeroStock() == null) {
            searchDto.setExcludeZeroStock(true); // 기본적으로 재고 0 제외
        }

        PageResponse<LocStockDto> page = inventoryService.getLocStockList(searchDto);
        model.addAttribute("page", page);
        model.addAttribute("stockList", page.getContent());
        model.addAttribute("search", searchDto);

        return "inventory/stock/list";
    }

    // 2. 재고 조정 팝업
    @GetMapping("/stock/{id}/adjust")
    public String stockAdjustForm(@PathVariable Long id, Model model) {
        model.addAttribute("active", "stock");
        LocStockDto stock = inventoryService.getLocStock(id);
        model.addAttribute("stock", stock);
        return "inventory/stock/adjust_form"; // 별도의 팝업 또는 폼
    }

    // 3. 재고 조정 처리
    @PostMapping("/stock/{id}/adjust")
    public String adjustStock(@PathVariable Long id, 
                              @RequestParam("adjustType") String adjustType,
                              @RequestParam("adjustQty") Double adjustQty,
                              @RequestParam(value = "transRmk", required = false) String transRmk,
                              RedirectAttributes redirectAttributes) {
        try {
            // ADJ_MINUS일 경우 음수로 변환
            double actualQty = "ADJ_MINUS".equals(adjustType) ? -Math.abs(adjustQty) : Math.abs(adjustQty);
            
            StockAdjustRequest request = new StockAdjustRequest(id, adjustType, actualQty, transRmk, "SYSTEM");
            inventoryService.adjustStock(request);
            redirectAttributes.addFlashAttribute("message", "재고 조정이 완료되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "재고 조정 실패: " + e.getMessage());
        }
        return "redirect:/inventory/stock";
    }

    // 4. 재고 트랜잭션 이력
    @GetMapping("/transactions")
    public String transactionList(LocTransHisSearchDto searchDto, Model model) {
        model.addAttribute("active", "transactions");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("transTypes", mesComCodeMapper.selectComCodeList("MCS_INV_TX_TYPE", "Y"));

        PageResponse<LocTransHisDto> page = inventoryService.getLocTransHisList(searchDto);
        model.addAttribute("page", page);
        model.addAttribute("transList", page.getContent());
        model.addAttribute("search", searchDto);

        return "inventory/transaction/list";
    }
}
