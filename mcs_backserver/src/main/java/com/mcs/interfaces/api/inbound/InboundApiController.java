package com.mcs.interfaces.api.inbound;

import com.mcs.application.service.inbound.InboundService;
import com.mcs.domain.inbound.dto.InboundItemDto;
import com.mcs.domain.inbound.dto.InboundOrderDto;
import com.mcs.domain.inbound.dto.InboundSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inbounds")
@RequiredArgsConstructor
public class InboundApiController {

    private final InboundService inboundService;

    @GetMapping
    public ApiResponse<PageResponse<InboundOrderDto>> getInboundList(InboundSearchDto searchDto) {
        return ApiResponse.ok(inboundService.getInboundList(searchDto));
    }

    @GetMapping("/{inboundId}")
    public ApiResponse<InboundOrderDto> getInbound(@PathVariable Long inboundId) {
        return ApiResponse.ok(inboundService.getInboundOrder(inboundId));
    }

    @GetMapping("/{inboundId}/items")
    public ApiResponse<List<InboundItemDto>> getInboundItems(@PathVariable Long inboundId) {
        return ApiResponse.ok(inboundService.getInboundItems(inboundId));
    }

    @PostMapping
    public ApiResponse<Long> createInbound(@RequestBody InboundOrderDto orderDto) {
        String inboundNo = orderDto.inboundNo();
        if (inboundNo == null || inboundNo.isBlank()) {
            inboundNo = "IB-" + System.currentTimeMillis();
        }

        InboundOrderDto dtoWithUser = new InboundOrderDto(
                null, orderDto.plantCd(), inboundNo, "PLANNED", orderDto.vendorCd(), orderDto.warehouseCd(),
                orderDto.expectedDt(), null, orderDto.receivePlanId(), orderDto.inboundRmk(),
                "SYSTEM", null, null, null, null, null, null, null
        );
        return ApiResponse.ok(inboundService.createInboundOrder(dtoWithUser));
    }

    @PutMapping("/{inboundId}")
    public ApiResponse<Void> updateInbound(@PathVariable Long inboundId, @RequestBody InboundOrderDto orderDto) {
        InboundOrderDto dtoWithId = new InboundOrderDto(
                inboundId, orderDto.plantCd(), orderDto.inboundNo(), orderDto.inboundStatus(), orderDto.vendorCd(), orderDto.warehouseCd(),
                orderDto.expectedDt(), orderDto.actualDt(), orderDto.receivePlanId(), orderDto.inboundRmk(),
                null, null, "SYSTEM", null, null, null, null, null
        );
        inboundService.updateInboundOrder(dtoWithId);
        return ApiResponse.ok();
    }

    @PostMapping("/{inboundId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long inboundId, @RequestParam String status) {
        inboundService.changeOrderStatus(inboundId, status, "SYSTEM");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{inboundId}")
    public ApiResponse<Void> deleteInbound(@PathVariable Long inboundId) {
        inboundService.deleteInboundOrder(inboundId);
        return ApiResponse.ok();
    }
}
