package com.mcs.interfaces.web.zone;

import com.mcs.application.service.zone.ZoneService;
import com.mcs.domain.zone.dto.ZoneDto;
import com.mcs.domain.zone.dto.ZoneSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesComCodeMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesPlantMapper;
import com.mcs.infra.persistence.mybatis.mapper.mes.MesWarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/zones")
@RequiredArgsConstructor
public class ZoneWebController {

    private final MesPlantMapper mesPlantMapper;
    private final MesWarehouseMapper mesWarehouseMapper;
    private final MesComCodeMapper mesComCodeMapper;
    private final ZoneService zoneService;

    @GetMapping
    public String zoneList(ZoneSearchDto searchDto, Model model) {
        model.addAttribute("active", "zones");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        
        PageResponse<ZoneDto> page = zoneService.getZoneList(searchDto);
        model.addAttribute("page", page);
        model.addAttribute("zonesList", page.getContent());
        model.addAttribute("search", searchDto);
        
        return "zone/list";
    }

    @GetMapping("/new")
    public String zoneFormNew(Model model) {
        model.addAttribute("active", "zones");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        model.addAttribute("zoneTypes", mesComCodeMapper.selectComCodeList("MCS_ZONE_TYPE", "Y"));
        
        model.addAttribute("mode", "create");
        // 빈 DTO 전달 (기본값 설정)
        ZoneDto emptyDto = new ZoneDto(null, null, null, null, null, null, 0, "Y", null, null, null, null, null, null, null);
        model.addAttribute("request", emptyDto);
        
        return "zone/form";
    }

    @PostMapping
    public String createZone(@ModelAttribute ZoneDto zoneDto, RedirectAttributes redirectAttributes) {
        try {
            ZoneDto dtoWithUser = new ZoneDto(
                null, zoneDto.plantCd(), zoneDto.warehouseCd(), zoneDto.zoneCd(),
                zoneDto.zoneNm(), zoneDto.zoneType(), zoneDto.sortSeq(), zoneDto.useYn(),
                "SYSTEM", null, null, null, null, null, null
            );
            zoneService.createZone(dtoWithUser);
            redirectAttributes.addFlashAttribute("message", "구역 정보가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 실패: " + e.toString());
        }
        return "redirect:/zones";
    }

    @GetMapping("/{id}/edit")
    public String zoneFormEdit(@PathVariable Long id, Model model) {
        model.addAttribute("active", "zones");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("warehouses", mesWarehouseMapper.selectWarehouseList(null, null, "Y"));
        model.addAttribute("zoneTypes", mesComCodeMapper.selectComCodeList("MCS_ZONE_TYPE", "Y"));
        
        model.addAttribute("mode", "edit");
        model.addAttribute("request", zoneService.getZone(id));
        
        return "zone/form";
    }

    @PostMapping("/{id}/edit")
    public String updateZone(@PathVariable Long id, @ModelAttribute ZoneDto zoneDto, RedirectAttributes redirectAttributes) {
        try {
            ZoneDto dtoWithId = new ZoneDto(
                id, zoneDto.plantCd(), zoneDto.warehouseCd(), zoneDto.zoneCd(),
                zoneDto.zoneNm(), zoneDto.zoneType(), zoneDto.sortSeq(), zoneDto.useYn(),
                null, null, "SYSTEM", null, null, null, null
            );
            zoneService.updateZone(dtoWithId);
            redirectAttributes.addFlashAttribute("message", "구역 정보가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수정 실패: " + e.toString());
        }
        return "redirect:/zones";
    }

    @PostMapping("/{id}/delete")
    public String deleteZone(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            zoneService.deleteZone(id);
            redirectAttributes.addFlashAttribute("message", "구역 정보가 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "삭제 실패: " + e.toString());
        }
        return "redirect:/zones";
    }
}
