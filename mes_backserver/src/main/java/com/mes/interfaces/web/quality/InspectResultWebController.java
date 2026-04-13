package com.mes.interfaces.web.quality;

import com.mes.application.service.quality.InspectResultService;
import com.mes.domain.quality.inspectresult.dto.InspectResultDto;
import com.mes.domain.quality.inspectresult.dto.InspectResultSearchDto;
import com.mes.global.common.dto.PageResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/quality/inspect-results")
public class InspectResultWebController {

    private final InspectResultService inspectResultService;

    public InspectResultWebController(InspectResultService inspectResultService) {
        this.inspectResultService = inspectResultService;
    }

    @GetMapping
    public String list(@ModelAttribute InspectResultSearchDto searchDto, Model model) {
        
        PageResponse<InspectResultDto> pageResponse = inspectResultService.getInspectResultPage(searchDto);
        model.addAttribute("results", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("active", "inspectResults");
        return "quality/inspect-result/list";
    }
}
