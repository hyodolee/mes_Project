package com.mes.interfaces.web.master;

import com.mes.application.service.master.CompanyService;
import com.mes.application.service.master.PlantService;
import com.mes.domain.master.plant.dto.PlantDto;
import com.mes.domain.master.plant.dto.PlantSearchDto;
import com.mes.domain.master.plant.dto.PlantUpsertRequest;
import com.mes.global.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/plants")
public class PlantWebController {

    private final PlantService plantService;
    private final CompanyService companyService;

    public PlantWebController(PlantService plantService, CompanyService companyService) {
        this.plantService = plantService;
        this.companyService = companyService;
    }

    @GetMapping
    public String list(@ModelAttribute PlantSearchDto searchDto, Model model) {
        PageResponse<PlantDto> pageResponse = plantService.getPlantList(searchDto);
        model.addAttribute("plants", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("companies", companyService.getCompanies(null, "Y"));
        model.addAttribute("plantNm", searchDto.getPlantNm());
        model.addAttribute("useYn", searchDto.getUseYn());
        model.addAttribute("active", "plants");
        return "master/plant/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new PlantUpsertRequest("", "", "", "", "", "Y"));
        model.addAttribute("companies", companyService.getCompanies(null, "Y"));
        model.addAttribute("mode", "create");
        return "master/plant/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") PlantUpsertRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("companies", companyService.getCompanies(null, "Y"));
            return "master/plant/form";
        }
        plantService.createPlant(request);
        redirectAttributes.addFlashAttribute("message", "공장 정보가 등록되었습니다.");
        return "redirect:/master/plants";
    }

    @GetMapping("/{plantCd}/edit")
    public String editForm(@PathVariable("plantCd") String plantCd, Model model) {
        var plant = plantService.getPlant(plantCd);
        model.addAttribute("request", new PlantUpsertRequest(
                plant.plantCd(),
                plant.companyCd(),
                plant.plantNm(),
                plant.addr(),
                plant.telNo(),
                plant.useYn()
        ));
        model.addAttribute("companies", companyService.getCompanies(null, "Y"));
        model.addAttribute("mode", "edit");
        return "master/plant/form";
    }

    @PostMapping("/{plantCd}")
    public String update(@PathVariable("plantCd") String plantCd,
                         @Valid @ModelAttribute("request") PlantUpsertRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("companies", companyService.getCompanies(null, "Y"));
            return "master/plant/form";
        }
        if (!plantCd.equals(request.plantCd())) {
            throw new IllegalArgumentException("경로의 plantCd와 폼의 plantCd가 일치하지 않습니다.");
        }
        plantService.updatePlant(request);
        redirectAttributes.addFlashAttribute("message", "공장 정보가 수정되었습니다.");
        return "redirect:/master/plants";
    }
}
