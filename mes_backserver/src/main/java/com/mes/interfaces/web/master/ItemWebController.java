package com.mes.interfaces.web.master;

import com.mes.application.service.master.ItemService;
import com.mes.application.service.master.PlantService;
import com.mes.domain.master.item.dto.ItemDto;
import com.mes.domain.master.item.dto.ItemSearchDto;
import com.mes.domain.master.item.dto.ItemUpsertRequest;
import com.mes.global.common.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/master/items")
public class ItemWebController {

    private final ItemService itemService;
    private final PlantService plantService;

    public ItemWebController(ItemService itemService, PlantService plantService) {
        this.itemService = itemService;
        this.plantService = plantService;
    }

    @GetMapping
    public String list(@ModelAttribute ItemSearchDto searchDto, Model model) {
        PageResponse<ItemDto> pageResponse = itemService.getItemList(searchDto);
        model.addAttribute("items", pageResponse.getContent());
        model.addAttribute("page", pageResponse);
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        model.addAttribute("itemNm", searchDto.getItemNm());
        model.addAttribute("useYn", searchDto.getUseYn());
        model.addAttribute("active", "items");
        return "master/item/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("request", new ItemUpsertRequest("", "", "", "", "", "", "", "Y"));
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        model.addAttribute("mode", "create");
        model.addAttribute("active", "items");
        return "master/item/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("request") ItemUpsertRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
            model.addAttribute("mode", "create");
            model.addAttribute("active", "items");
            return "master/item/form";
        }
        itemService.createItem(request);
        redirectAttributes.addFlashAttribute("message", "품목 정보가 등록되었습니다.");
        return "redirect:/master/items";
    }

    @GetMapping("/{itemCd}/edit")
    public String editForm(@PathVariable("itemCd") String itemCd, Model model) {
        var item = itemService.getItem(itemCd);
        model.addAttribute("request", new ItemUpsertRequest(
                item.itemCd(),
                item.plantCd(),
                item.itemNm(),
                item.itemSpec(),
                item.unit(),
                item.itemType(),
                item.itemGrp(),
                item.useYn()
        ));
        model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
        model.addAttribute("mode", "edit");
        model.addAttribute("active", "items");
        return "master/item/form";
    }

    @PostMapping("/{itemCd}")
    public String update(@PathVariable("itemCd") String itemCd,
                         @Valid @ModelAttribute("request") ItemUpsertRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("plants", plantService.getPlants(null, null, "Y"));
            model.addAttribute("mode", "edit");
            model.addAttribute("active", "items");
            return "master/item/form";
        }
        if (!itemCd.equals(request.itemCd())) {
            throw new IllegalArgumentException("경로의 itemCd와 폼의 itemCd가 일치하지 않습니다.");
        }
        itemService.updateItem(request);
        redirectAttributes.addFlashAttribute("message", "품목 정보가 수정되었습니다.");
        return "redirect:/master/items";
    }
}
