package com.mcs.interfaces.web.inbound;

import com.mcs.application.service.inbound.InboundService;
import com.mcs.domain.inbound.dto.InboundItemDto;
import com.mcs.domain.inbound.dto.InboundOrderDto;
import com.mcs.domain.inbound.dto.InboundSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesComCodeMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesPlantMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/inbounds")
@RequiredArgsConstructor
public class InboundWebController {

    private final InboundService inboundService;
    private final MesPlantMapper mesPlantMapper;
    private final MesWarehouseMapper mesWarehouseMapper;
    private final MesComCodeMapper mesComCodeMapper;

    @GetMapping
    public String inboundList(InboundSearchDto searchDto, Model model) {
        model.addAttribute("active", "inbounds");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        model.addAttribute("ibStatuses", mesComCodeMapper.selectComCodeList("MCS_IB_STATUS", "Y"));
        
        PageResponse<InboundOrderDto> page = inboundService.getInboundList(searchDto);
        model.addAttribute("page", page);
        model.addAttribute("inboundsList", page.getContent());
        model.addAttribute("search", searchDto);
        
        return "inbound/list";
    }

    @GetMapping("/new")
    public String inboundFormNew(Model model) {
        model.addAttribute("active", "inbounds");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        
        model.addAttribute("mode", "create");
        InboundOrderDto emptyDto = new InboundOrderDto(null, null, null, "PLANNED", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        model.addAttribute("request", emptyDto);
        
        return "inbound/form";
    }

    @PostMapping
    public String createInbound(@ModelAttribute InboundOrderDto orderDto, RedirectAttributes redirectAttributes) {
        try {
            // Generate a dummy inbound number for phase 3 example
            String autoInboundNo = "IB-" + System.currentTimeMillis();
            
            InboundOrderDto dtoWithUser = new InboundOrderDto(
                null, orderDto.plantCd(), autoInboundNo, "PLANNED", orderDto.vendorCd(), orderDto.warehouseCd(),
                orderDto.expectedDt(), null, null, orderDto.inboundRmk(),
                "SYSTEM", null, null, null, null, null, null, null
            );
            inboundService.createInboundOrder(dtoWithUser);
            redirectAttributes.addFlashAttribute("message", "입고 오더가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 실패: " + e.toString());
        }
        return "redirect:/inbounds";
    }

    @GetMapping("/{id}/edit")
    public String inboundFormEdit(@PathVariable Long id, Model model) {
        model.addAttribute("active", "inbounds");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        model.addAttribute("ibStatuses", mesComCodeMapper.selectComCodeList("MCS_IB_STATUS", "Y"));
        
        model.addAttribute("mode", "edit");
        InboundOrderDto order = inboundService.getInboundOrder(id);
        List<InboundItemDto> items = inboundService.getInboundItems(id);
        model.addAttribute("request", order);
        model.addAttribute("items", items);
        
        return "inbound/form";
    }

    @PostMapping("/{id}/edit")
    public String updateInbound(@PathVariable Long id, @ModelAttribute InboundOrderDto orderDto, RedirectAttributes redirectAttributes) {
        try {
            InboundOrderDto dtoWithId = new InboundOrderDto(
                id, orderDto.plantCd(), orderDto.inboundNo(), orderDto.inboundStatus(), orderDto.vendorCd(), orderDto.warehouseCd(),
                orderDto.expectedDt(), orderDto.actualDt(), null, orderDto.inboundRmk(),
                null, null, "SYSTEM", null, null, null, null, null
            );
            inboundService.updateInboundOrder(dtoWithId);
            redirectAttributes.addFlashAttribute("message", "입고 오더가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수정 실패: " + e.toString());
        }
        return "redirect:/inbounds";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes) {
        try {
            inboundService.changeOrderStatus(id, status, "SYSTEM");
            redirectAttributes.addFlashAttribute("message", "상태가 변경되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "상태 변경 실패: " + e.toString());
        }
        return "redirect:/inbounds/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String deleteInbound(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            inboundService.deleteInboundOrder(id);
            redirectAttributes.addFlashAttribute("message", "입고 오더가 삭제되었습니다.");
            return "redirect:/inbounds";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "삭제 실패: " + e.toString());
            return "redirect:/inbounds/" + id + "/edit";
        }
    }
}
