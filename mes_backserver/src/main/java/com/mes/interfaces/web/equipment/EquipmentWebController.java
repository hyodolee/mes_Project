package com.mes.interfaces.web.equipment;

import com.mes.application.service.equipment.EquipmentService;
import com.mes.domain.equipment.oper.dto.OperStatusDto;
import com.mes.domain.equipment.oper.dto.OperStatusSearchDto;
import com.mes.global.common.dto.PageResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/equipment/oper-statuses")
public class EquipmentWebController {

    private final EquipmentService equipmentService;

    public EquipmentWebController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public String list(@ModelAttribute OperStatusSearchDto searchDto, Model model) {
        
        PageResponse<OperStatusDto> pageResponse = equipmentService.getOperStatusPage(searchDto);
        model.addAttribute("statuses", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("active", "operStatuses");
        return "equipment/oper-status/list";
    }
}
