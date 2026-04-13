package com.mes.interfaces.web.production;

import com.mes.application.service.production.WorkResultService;
import com.mes.domain.production.workresult.dto.WorkResultDto;
import com.mes.domain.production.workresult.dto.WorkResultSearchDto;
import com.mes.global.common.dto.PageResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/production/work-results")
public class WorkResultWebController {

    private final WorkResultService workResultService;

    public WorkResultWebController(WorkResultService workResultService) {
        this.workResultService = workResultService;
    }

    @GetMapping
    public String list(@ModelAttribute WorkResultSearchDto searchDto, Model model) {
        
        PageResponse<WorkResultDto> pageResponse = workResultService.getWorkResultPage(searchDto);
        model.addAttribute("results", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("active", "workResults");
        return "production/work-result/list";
    }
}
