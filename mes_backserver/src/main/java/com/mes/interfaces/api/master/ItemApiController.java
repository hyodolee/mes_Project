package com.mes.interfaces.api.master;

import com.mes.application.service.master.ItemService;
import com.mes.domain.master.item.dto.ItemDto;
import com.mes.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/master/items")
public class ItemApiController {

    private final ItemService itemService;

    public ItemApiController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ApiResponse<List<ItemDto>> getItems(
            @RequestParam(name = "itemNm", required = false) String itemNm,
            @RequestParam(name = "useYn", required = false) String useYn
    ) {
        return ApiResponse.ok(itemService.getItems(itemNm, useYn));
    }
}
