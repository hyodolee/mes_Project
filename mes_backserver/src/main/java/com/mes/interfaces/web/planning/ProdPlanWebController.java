package com.mes.interfaces.web.planning;

import com.mes.application.service.master.PlantService;
import com.mes.application.service.planning.ProdPlanService;
import com.mes.domain.planning.prodplan.dto.ProdPlanCreateRequest;
import com.mes.domain.planning.prodplan.dto.ProdPlanDto;
import com.mes.domain.planning.prodplan.dto.ProdPlanSearchDto;
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

@Controller
@RequestMapping("/planning/prod-plans")
public class ProdPlanWebController {

    private final ProdPlanService prodPlanService;
    private final PlantService plantService;

    public ProdPlanWebController(ProdPlanService prodPlanService, PlantService plantService) {
        this.prodPlanService = prodPlanService;
        this.plantService = plantService;
    }

    @GetMapping
    public String list(@ModelAttribute ProdPlanSearchDto searchDto, Model model) {
        PageResponse<ProdPlanDto> pageResponse = prodPlanService.getProdPlanList(searchDto);
        model.addAttribute("plans", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        model.addAttribute("plantCd", searchDto.getPlantCd());
        model.addAttribute("itemCd", searchDto.getItemCd());
        model.addAttribute("planStatus", searchDto.getPlanStatus());
        model.addAttribute("planFromDt", searchDto.getPlanFromDt());
        model.addAttribute("planToDt", searchDto.getPlanToDt());
        return "planning/prod-plan/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new ProdPlanCreateRequest(
                "", "", LocalDate.now(), "", "", BigDecimal.ONE,
                LocalDate.now(), LocalDate.now(), 5, "", "", "",
                null, "계획", ""
        ));
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        return "planning/prod-plan/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") ProdPlanCreateRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
            return "planning/prod-plan/form";
        }
        prodPlanService.createProdPlan(request);
        redirectAttributes.addFlashAttribute("message", "생산계획이 등록되었습니다.");
        return "redirect:/planning/prod-plans";
    }
}
