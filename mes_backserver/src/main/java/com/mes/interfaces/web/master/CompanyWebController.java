package com.mes.interfaces.web.master;

import com.mes.application.service.master.CompanyService;
import com.mes.domain.master.company.dto.CompanyDto;
import com.mes.domain.master.company.dto.CompanySearchDto;
import com.mes.domain.master.company.dto.CompanyUpsertRequest;
import com.mes.global.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/companies")
public class CompanyWebController {

    private final CompanyService companyService;

    public CompanyWebController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public String list(@ModelAttribute CompanySearchDto searchDto, Model model) {
        PageResponse<CompanyDto> pageResponse = companyService.getCompanyList(searchDto);
        model.addAttribute("companies", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("companyNm", searchDto.getCompanyNm());
        model.addAttribute("useYn", searchDto.getUseYn());
        model.addAttribute("active", "companies");
        return "master/company/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new CompanyUpsertRequest("", "", "", "", "", "", "Y"));
        model.addAttribute("mode", "create");
        return "master/company/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") CompanyUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "master/company/form";
        }
        companyService.createCompany(request);
        redirectAttributes.addFlashAttribute("message", "회사 정보가 등록되었습니다.");
        return "redirect:/master/companies";
    }

    @GetMapping("/{companyCd}/edit")
    public String editForm(@PathVariable("companyCd") String companyCd, Model model) {
        var company = companyService.getCompany(companyCd);
        model.addAttribute("request", new CompanyUpsertRequest(
                company.companyCd(),
                company.companyNm(),
                company.bizNo(),
                company.ceoNm(),
                company.addr(),
                company.telNo(),
                company.useYn()));
        model.addAttribute("mode", "edit");
        return "master/company/form";
    }

    @PostMapping("/{companyCd}")
    public String update(@PathVariable("companyCd") String companyCd,
            @Valid @ModelAttribute("request") CompanyUpsertRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "master/company/form";
        }
        if (!companyCd.equals(request.companyCd())) {
            throw new IllegalArgumentException("경로의 companyCd와 폼의 companyCd가 일치하지 않습니다.");
        }
        companyService.updateCompany(request);
        redirectAttributes.addFlashAttribute("message", "회사 정보가 수정되었습니다.");
        return "redirect:/master/companies";
    }
}
