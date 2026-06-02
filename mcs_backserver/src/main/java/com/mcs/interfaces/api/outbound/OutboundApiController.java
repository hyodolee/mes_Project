package com.mcs.interfaces.api.outbound;

import com.mcs.application.service.outbound.OutboundService;
import com.mcs.domain.outbound.dto.OutboundItemDto;
import com.mcs.domain.outbound.dto.OutboundOrderDto;
import com.mcs.domain.outbound.dto.OutboundSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/outbounds")
@RequiredArgsConstructor
public class OutboundApiController {

    private final OutboundService outboundService;

    @GetMapping
    public ApiResponse<PageResponse<OutboundOrderDto>> getOutboundList(OutboundSearchDto searchDto) {
        return ApiResponse.ok(outboundService.getOutboundList(searchDto));
    }

    @GetMapping("/{outboundId}")
    public ApiResponse<OutboundOrderDto> getOutbound(@PathVariable Long outboundId) {
        return ApiResponse.ok(outboundService.getOutboundOrder(outboundId));
    }

    @GetMapping("/{outboundId}/items")
    public ApiResponse<List<OutboundItemDto>> getOutboundItems(@PathVariable Long outboundId) {
        return ApiResponse.ok(outboundService.getOutboundItems(outboundId));
    }

    @PostMapping
    public ApiResponse<Long> createOutbound(@RequestBody OutboundOrderDto orderDto) {
        String outboundNo = orderDto.outboundNo();
        if (outboundNo == null || outboundNo.isBlank()) {
            outboundNo = "OB-" + System.currentTimeMillis();
        }

        OutboundOrderDto dtoWithUser = new OutboundOrderDto(
                null, orderDto.plantCd(), outboundNo, "REQUESTED", orderDto.customerCd(), orderDto.warehouseCd(),
                orderDto.requestDt(), null, orderDto.destination(), orderDto.issuePlanId(), orderDto.woId(), orderDto.outboundRmk(),
                "SYSTEM", null, null, null, null, null, null
        );
        return ApiResponse.ok(outboundService.createOutboundOrder(dtoWithUser));
    }

    @PutMapping("/{outboundId}")
    public ApiResponse<Void> updateOutbound(@PathVariable Long outboundId, @RequestBody OutboundOrderDto orderDto) {
        OutboundOrderDto dtoWithId = new OutboundOrderDto(
                outboundId, orderDto.plantCd(), orderDto.outboundNo(), orderDto.outboundStatus(), orderDto.customerCd(), orderDto.warehouseCd(),
                orderDto.requestDt(), orderDto.shippedDt(), orderDto.destination(), orderDto.issuePlanId(), orderDto.woId(), orderDto.outboundRmk(),
                null, null, "SYSTEM", null, null, null, null
        );
        outboundService.updateOutboundOrder(dtoWithId);
        return ApiResponse.ok();
    }

    @PostMapping("/{outboundId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable Long outboundId, @RequestParam String status) {
        outboundService.changeOrderStatus(outboundId, status, "SYSTEM");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{outboundId}")
    public ApiResponse<Void> deleteOutbound(@PathVariable Long outboundId) {
        outboundService.deleteOutboundOrder(outboundId);
        return ApiResponse.ok();
    }
}
