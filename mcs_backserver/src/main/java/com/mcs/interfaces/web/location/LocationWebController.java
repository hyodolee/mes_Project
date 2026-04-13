package com.mcs.interfaces.web.location;

import com.mcs.application.service.location.LocationService;
import com.mcs.application.service.zone.ZoneService;
import com.mcs.domain.location.dto.LocationDto;
import com.mcs.domain.location.dto.LocationSearchDto;
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
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationWebController {

    private final MesPlantMapper mesPlantMapper;
    private final MesWarehouseMapper mesWarehouseMapper;
    private final MesComCodeMapper mesComCodeMapper;
    private final ZoneService zoneService;
    private final LocationService locationService;

    @GetMapping
    public String locationList(LocationSearchDto searchDto, Model model) {
        model.addAttribute("active", "locations");
        model.addAttribute("plants", mesPlantMapper.selectPlantList());
        model.addAttribute("zones", zoneService.getZoneList(new ZoneSearchDto()).getContent());
        
        PageResponse<LocationDto> page = locationService.getLocationList(searchDto);
        model.addAttribute("page", page);
        model.addAttribute("locationsList", page.getContent());
        model.addAttribute("search", searchDto);
        
        return "location/list";
    }

    @GetMapping("/new")
    public String locationFormNew(Model model) {
        model.addAttribute("active", "locations");
        model.addAttribute("zones", zoneService.getZoneList(new ZoneSearchDto()).getContent());
        model.addAttribute("locStatuses", mesComCodeMapper.selectComCodeList("MCS_LOC_STATUS", "Y"));
        
        model.addAttribute("mode", "create");
        LocationDto emptyDto = new LocationDto(null, null, null, null, 0.0, 0.0, "EMPTY", "Y", null, null, null, null, null, null, null, null, null, null, null);
        model.addAttribute("request", emptyDto);
        
        return "location/form";
    }

    @PostMapping
    public String createLocation(@ModelAttribute LocationDto locationDto, RedirectAttributes redirectAttributes) {
        try {
            LocationDto dtoWithUser = new LocationDto(
                null, locationDto.zoneId(), locationDto.locationCd(), locationDto.locationNm(),
                locationDto.maxCapacity(), 0.0, "EMPTY", locationDto.useYn(),
                "SYSTEM", null, null, null, null, null, null, null, null, null, null
            );
            locationService.createLocation(dtoWithUser);
            redirectAttributes.addFlashAttribute("message", "로케이션 정보가 등록되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "등록 실패: " + e.toString());
        }
        return "redirect:/locations";
    }

    @GetMapping("/{id}/edit")
    public String locationFormEdit(@PathVariable Long id, Model model) {
        model.addAttribute("active", "locations");
        model.addAttribute("zones", zoneService.getZoneList(new ZoneSearchDto()).getContent());
        model.addAttribute("locStatuses", mesComCodeMapper.selectComCodeList("MCS_LOC_STATUS", "Y"));
        
        model.addAttribute("mode", "edit");
        model.addAttribute("request", locationService.getLocation(id));
        
        return "location/form";
    }

    @PostMapping("/{id}/edit")
    public String updateLocation(@PathVariable Long id, @ModelAttribute LocationDto locationDto, RedirectAttributes redirectAttributes) {
        try {
            LocationDto dtoWithId = new LocationDto(
                id, locationDto.zoneId(), locationDto.locationCd(), locationDto.locationNm(),
                locationDto.maxCapacity(), null, locationDto.locationStatus(), locationDto.useYn(),
                null, null, "SYSTEM", null, null, null, null, null, null, null, null
            );
            locationService.updateLocation(dtoWithId);
            redirectAttributes.addFlashAttribute("message", "로케이션 정보가 수정되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수정 실패: " + e.getMessage());
        }
        return "redirect:/locations";
    }

    @PostMapping("/{id}/delete")
    public String deleteLocation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            locationService.deleteLocation(id);
            redirectAttributes.addFlashAttribute("message", "로케이션 정보가 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "삭제 실패: " + e.getMessage());
        }
        return "redirect:/locations";
    }
}
