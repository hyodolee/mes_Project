package com.mcs.interfaces.web.outbound;

import com.mcs.application.service.outbound.OutboundService;
import com.mcs.domain.outbound.dto.OutboundItemDto;
import com.mcs.domain.outbound.dto.OutboundOrderDto;
import com.mcs.domain.outbound.dto.OutboundSearchDto;
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
@RequestMapping("/outbounds")
@RequiredArgsConstructor
public class OutboundWebController {

    private final OutboundService outboundService;
    private final MesPlantMapper mesPlantMapper;
    private final MesWarehouseMapper mesWarehouseMapper;
    private final MesComCodeMapper mesComCodeMapper;

    @GetMapping
    public String outboundList(OutboundSearchDto searchDto, Model model) {
        model.addAttribute("active", "outbounds");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        model.addAttribute("obStatuses", mesComCodeMapper.selectComCodeList("MCS_OB_STATUS", "Y"));
        
        PageResponse<OutboundOrderDto> page = outboundService.getOutboundList(searchDto);
        model.addAttribute("page", page);
        model.addAttribute("outboundsList", page.getContent());
        model.addAttribute("search", searchDto);
        
        return "outbound/list";
    }

    @GetMapping("/new")
    public String outboundFormNew(Model model) {
        model.addAttribute("active", "outbounds");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        
        model.addAttribute("mode", "create");
        OutboundOrderDto emptyDto = new OutboundOrderDto(null, null, null, "REQUESTED", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        model.addAttribute("request", emptyDto);
        
        return "outbound/form";
    }

    @PostMapping
    public String createOutbound(@ModelAttribute OutboundOrderDto orderDto, RedirectAttributes redirectAttributes) {
        try {
            // Generate a dummy outbound number for phase 4 example
            String autoOutboundNo = "OB-" + System.currentTimeMillis();
            
            OutboundOrderDto dtoWithUser = new OutboundOrderDto(
                null, orderDto.plantCd(), autoOutboundNo, "REQUESTED", orderDto.customerCd(), orderDto.warehouseCd(),
                orderDto.requestDt(), null, orderDto.destination(), null, null, orderDto.outboundRmk(),
                "SYSTEM", null, null, null, null, null, null
            );
            outboundService.createOutboundOrder(dtoWithUser);
            redirectAttributes.addFlashAttribute("message", "출고 오더가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 실패: " + e.toString());
        }
        return "redirect:/outbounds";
    }

    @GetMapping("/{id}/edit")
    public String outboundFormEdit(@PathVariable Long id, Model model) {
        model.addAttribute("active", "outbounds");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        model.addAttribute("obStatuses", mesComCodeMapper.selectComCodeList("MCS_OB_STATUS", "Y"));
        
        model.addAttribute("mode", "edit");
        OutboundOrderDto order = outboundService.getOutboundOrder(id);
        List<OutboundItemDto> items = outboundService.getOutboundItems(id);
        model.addAttribute("request", order);
        model.addAttribute("items", items);
        
        return "outbound/form";
    }

    @PostMapping("/{id}/edit")
    public String updateOutbound(@PathVariable Long id, @ModelAttribute OutboundOrderDto orderDto, RedirectAttributes redirectAttributes) {
        try {
            OutboundOrderDto dtoWithId = new OutboundOrderDto(
                id, orderDto.plantCd(), orderDto.outboundNo(), orderDto.outboundStatus(), orderDto.customerCd(), orderDto.warehouseCd(),
                orderDto.requestDt(), orderDto.shippedDt(), orderDto.destination(), null, null, orderDto.outboundRmk(),
                null, null, "SYSTEM", null, null, null, null
            );
            outboundService.updateOutboundOrder(dtoWithId);
            redirectAttributes.addFlashAttribute("message", "출고 오더가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수정 실패: " + e.toString());
        }
        return "redirect:/outbounds";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes) {
        try {
            outboundService.changeOrderStatus(id, status, "SYSTEM");
            redirectAttributes.addFlashAttribute("message", "상태가 변경되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "상태 변경 실패: " + e.toString());
        }
        return "redirect:/outbounds/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String deleteOutbound(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            outboundService.deleteOutboundOrder(id);
            redirectAttributes.addFlashAttribute("message", "출고 오더가 삭제되었습니다.");
            return "redirect:/outbounds";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "삭제 실패: " + e.toString());
            return "redirect:/outbounds/" + id + "/edit";
        }
    }
}
