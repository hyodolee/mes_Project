package com.mes.interfaces.web.planning;

import com.mes.application.service.master.PlantService;
import com.mes.application.service.planning.WorkOrderService;
import com.mes.domain.planning.workorder.dto.WorkOrderCreateRequest;
import com.mes.domain.planning.workorder.dto.WorkOrderDto;
import com.mes.domain.planning.workorder.dto.WorkOrderSearchDto;
import com.mes.global.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/planning/work-orders")
public class WorkOrderWebController {

    private final WorkOrderService workOrderService;
    private final PlantService plantService;

    public WorkOrderWebController(WorkOrderService workOrderService, PlantService plantService) {
        this.workOrderService = workOrderService;
        this.plantService = plantService;
    }

    @GetMapping
    public String list(@ModelAttribute WorkOrderSearchDto searchDto, Model model) {
        PageResponse<WorkOrderDto> pageResponse = workOrderService.getWorkOrderList(searchDto);
        model.addAttribute("workOrders", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        model.addAttribute("plantCd", searchDto.getPlantCd());
        model.addAttribute("itemCd", searchDto.getItemCd());
        model.addAttribute("woStatus", searchDto.getWoStatus());
        model.addAttribute("woFromDt", searchDto.getWoFromDt());
        model.addAttribute("woToDt", searchDto.getWoToDt());
        return "planning/work-order/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new WorkOrderCreateRequest(
                "", null, LocalDate.now(), "", BigDecimal.ONE,
                "", "", "", LocalDateTime.now(), LocalDateTime.now(),
                5, "", "", null, ""
        ));
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        return "planning/work-order/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") WorkOrderCreateRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
            return "planning/work-order/form";
        }
        workOrderService.createWorkOrder(request);
        redirectAttributes.addFlashAttribute("message", "작업지시가 등록되었습니다.");
        return "redirect:/planning/work-orders";
    }
}
