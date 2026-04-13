package com.mcs.interfaces.web.transfer;

import com.mcs.application.service.transfer.TransferService;
import com.mcs.domain.transfer.dto.TransferItemDto;
import com.mcs.domain.transfer.dto.TransferOrderDto;
import com.mcs.domain.transfer.dto.TransferSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesComCodeMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesPlantMapper;
import com.mcs.application.service.location.LocationService;
import com.mcs.domain.location.dto.LocationSearchDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferWebController {

    private final TransferService transferService;
    private final LocationService locationService;
    private final MesPlantMapper mesPlantMapper;
    private final MesComCodeMapper mesComCodeMapper;

    @GetMapping
    public String transferList(TransferSearchDto searchDto, Model model) {
        model.addAttribute("active", "transfers");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("tfStatuses", mesComCodeMapper.selectComCodeList("MCS_TF_STATUS", "Y"));
        
        PageResponse<TransferOrderDto> page = transferService.getTransferList(searchDto);
        model.addAttribute("page", page);
        model.addAttribute("transfersList", page.getContent());
        model.addAttribute("search", searchDto);
        
        return "transfer/list";
    }

    @GetMapping("/new")
    public String transferFormNew(Model model) {
        model.addAttribute("active", "transfers");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        
        // 이동 가능한 모든 로케이션 목록 가져오기
        model.addAttribute("locations", locationService.getLocationList(new LocationSearchDto()).getContent());
        
        model.addAttribute("mode", "create");
        TransferOrderDto emptyDto = new TransferOrderDto(null, null, null, "REQUESTED", null, null, null, null, null, null, null, null, null, null, null);
        model.addAttribute("request", emptyDto);
        
        return "transfer/form";
    }

    @PostMapping
    public String createTransfer(@ModelAttribute TransferOrderDto orderDto, RedirectAttributes redirectAttributes) {
        try {
            String autoTransferNo = "TF-" + System.currentTimeMillis();
            
            TransferOrderDto dtoWithUser = new TransferOrderDto(
                null, orderDto.plantCd(), autoTransferNo, "REQUESTED", orderDto.fromLocationId(), orderDto.toLocationId(),
                orderDto.transferReason(), "SYSTEM", null, null, null, null, null, null, null
            );
            transferService.createTransferOrder(dtoWithUser);
            redirectAttributes.addFlashAttribute("message", "이동 오더가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 실패: " + e.toString());
        }
        return "redirect:/transfers";
    }

    @GetMapping("/{id}/edit")
    public String transferFormEdit(@PathVariable Long id, Model model) {
        model.addAttribute("active", "transfers");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("tfStatuses", mesComCodeMapper.selectComCodeList("MCS_TF_STATUS", "Y"));
        model.addAttribute("locations", locationService.getLocationList(new LocationSearchDto()).getContent());
        
        model.addAttribute("mode", "edit");
        TransferOrderDto order = transferService.getTransferOrder(id);
        List<TransferItemDto> items = transferService.getTransferItems(id);
        model.addAttribute("request", order);
        model.addAttribute("items", items);
        
        return "transfer/form";
    }

    @PostMapping("/{id}/edit")
    public String updateTransfer(@PathVariable Long id, @ModelAttribute TransferOrderDto orderDto, RedirectAttributes redirectAttributes) {
        try {
            TransferOrderDto dtoWithId = new TransferOrderDto(
                id, orderDto.plantCd(), orderDto.transferNo(), orderDto.transferStatus(), orderDto.fromLocationId(), orderDto.toLocationId(),
                orderDto.transferReason(), null, null, "SYSTEM", null, null, null, null, null
            );
            transferService.updateTransferOrder(dtoWithId);
            redirectAttributes.addFlashAttribute("message", "이동 오더가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수정 실패: " + e.toString());
        }
        return "redirect:/transfers";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam("status") String status, RedirectAttributes redirectAttributes) {
        try {
            transferService.changeOrderStatus(id, status, "SYSTEM");
            redirectAttributes.addFlashAttribute("message", "상태가 변경되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "상태 변경 실패: " + e.toString());
        }
        return "redirect:/transfers/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String deleteTransfer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            transferService.deleteTransferOrder(id);
            redirectAttributes.addFlashAttribute("message", "이동 오더가 삭제되었습니다.");
            return "redirect:/transfers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "삭제 실패: " + e.toString());
            return "redirect:/transfers/" + id + "/edit";
        }
    }
}
