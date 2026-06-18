package com.mes.mcs.interfaces.api.inbound;

import com.mes.mcs.application.service.inbound.InboundService;
import com.mes.mcs.domain.inbound.dto.InboundItemDto;
import com.mes.mcs.domain.inbound.dto.InboundOrderDto;
import com.mes.mcs.domain.inbound.dto.InboundSearchDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("mcsInboundApiController")
@RequestMapping("/api/inbounds")
@RequiredArgsConstructor
public class InboundApiController {

    private final InboundService inboundService;

    @GetMapping
    public ApiResponse<PageResponse<InboundOrderDto>> getInboundList(InboundSearchDto searchDto) {
        return ApiResponse.ok(inboundService.getInboundList(searchDto));
    }

    @GetMapping("/{inboundId}")
    public ApiResponse<InboundOrderDto> getInbound(@PathVariable("inboundId") Long inboundId) {
        return ApiResponse.ok(inboundService.getInboundOrder(inboundId));
    }

    @GetMapping("/{inboundId}/items")
    public ApiResponse<List<InboundItemDto>> getInboundItems(@PathVariable("inboundId") Long inboundId) {
        return ApiResponse.ok(inboundService.getInboundItems(inboundId));
    }

    @PostMapping
    public ApiResponse<Long> createInbound(@RequestBody InboundOrderDto orderDto) {
        String inboundNo = orderDto.getInboundNo();
        if (inboundNo == null || inboundNo.isBlank()) {
            inboundNo = "IB-" + System.currentTimeMillis();
        }

        InboundOrderDto dtoWithUser = new InboundOrderDto(
                null, orderDto.getPlantCd(), inboundNo, "PLANNED", orderDto.getVendorCd(), orderDto.getWarehouseCd(),
                orderDto.getExpectedDt(), null, orderDto.getReceivePlanId(), orderDto.getInboundRmk(),
                "SYSTEM", null, null, null, null, null, null, null
        );
        return ApiResponse.ok(inboundService.createInboundOrder(dtoWithUser));
    }

    @PutMapping("/{inboundId}")
    public ApiResponse<Void> updateInbound(@PathVariable("inboundId") Long inboundId, @RequestBody InboundOrderDto orderDto) {
        InboundOrderDto dtoWithId = new InboundOrderDto(
                inboundId, orderDto.getPlantCd(), orderDto.getInboundNo(), orderDto.getInboundStatus(), orderDto.getVendorCd(), orderDto.getWarehouseCd(),
                orderDto.getExpectedDt(), orderDto.getActualDt(), orderDto.getReceivePlanId(), orderDto.getInboundRmk(),
                null, null, "SYSTEM", null, null, null, null, null
        );
        inboundService.updateInboundOrder(dtoWithId);
        return ApiResponse.ok();
    }

    @PostMapping("/{inboundId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable("inboundId") Long inboundId, @RequestParam("status") String status) {
        inboundService.changeOrderStatus(inboundId, status, "SYSTEM");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{inboundId}")
    public ApiResponse<Void> deleteInbound(@PathVariable("inboundId") Long inboundId) {
        inboundService.deleteInboundOrder(inboundId);
        return ApiResponse.ok();
    }
}
