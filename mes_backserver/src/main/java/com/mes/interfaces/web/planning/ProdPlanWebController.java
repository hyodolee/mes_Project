package com.mes.interfaces.web.planning;

import com.mes.application.service.master.PlantService;
import com.mes.application.service.planning.ProdPlanService;
import com.mes.domain.planning.prodplan.dto.ProdPlanCreateRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public String list(@RequestParam(name = "plantCd", required = false) String plantCd,
                       @RequestParam(name = "itemCd", required = false) String itemCd,
                       @RequestParam(name = "planStatus", required = false) String planStatus,
                       @RequestParam(name = "planFromDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planFromDt,
                       @RequestParam(name = "planToDt", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate planToDt,
                       Model model) {
        model.addAttribute("plans", prodPlanService.getProdPlans(plantCd, itemCd, planStatus, planFromDt, planToDt));
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        model.addAttribute("plantCd", plantCd);
        model.addAttribute("itemCd", itemCd);
        model.addAttribute("planStatus", planStatus);
        model.addAttribute("planFromDt", planFromDt);
        model.addAttribute("planToDt", planToDt);
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
