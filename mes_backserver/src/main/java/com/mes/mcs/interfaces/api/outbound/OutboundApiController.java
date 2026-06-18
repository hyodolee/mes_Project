package com.mes.mcs.interfaces.api.outbound;

import com.mes.mcs.application.service.outbound.OutboundService;
import com.mes.mcs.domain.outbound.dto.OutboundItemDto;
import com.mes.mcs.domain.outbound.dto.OutboundOrderDto;
import com.mes.mcs.domain.outbound.dto.OutboundSearchDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("mcsOutboundApiController")
@RequestMapping("/api/outbounds")
@RequiredArgsConstructor
public class OutboundApiController {

    private final OutboundService outboundService;

    @GetMapping
    public ApiResponse<PageResponse<OutboundOrderDto>> getOutboundList(OutboundSearchDto searchDto) {
        return ApiResponse.ok(outboundService.getOutboundList(searchDto));
    }

    @GetMapping("/{outboundId}")
    public ApiResponse<OutboundOrderDto> getOutbound(@PathVariable("outboundId") Long outboundId) {
        return ApiResponse.ok(outboundService.getOutboundOrder(outboundId));
    }

    @GetMapping("/{outboundId}/items")
    public ApiResponse<List<OutboundItemDto>> getOutboundItems(@PathVariable("outboundId") Long outboundId) {
        return ApiResponse.ok(outboundService.getOutboundItems(outboundId));
    }

    @PostMapping
    public ApiResponse<Long> createOutbound(@RequestBody OutboundOrderDto orderDto) {
        String outboundNo = orderDto.getOutboundNo();
        if (outboundNo == null || outboundNo.isBlank()) {
            outboundNo = "OB-" + System.currentTimeMillis();
        }

        OutboundOrderDto dtoWithUser = new OutboundOrderDto(
                null, orderDto.getPlantCd(), outboundNo, "REQUESTED", orderDto.getCustomerCd(), orderDto.getWarehouseCd(),
                orderDto.getRequestDt(), null, orderDto.getDestination(), orderDto.getIssuePlanId(), orderDto.getWoId(), orderDto.getOutboundRmk(),
                "SYSTEM", null, null, null, null, null, null
        );
        return ApiResponse.ok(outboundService.createOutboundOrder(dtoWithUser));
    }

    @PutMapping("/{outboundId}")
    public ApiResponse<Void> updateOutbound(@PathVariable("outboundId") Long outboundId, @RequestBody OutboundOrderDto orderDto) {
        OutboundOrderDto dtoWithId = new OutboundOrderDto(
                outboundId, orderDto.getPlantCd(), orderDto.getOutboundNo(), orderDto.getOutboundStatus(), orderDto.getCustomerCd(), orderDto.getWarehouseCd(),
                orderDto.getRequestDt(), orderDto.getShippedDt(), orderDto.getDestination(), orderDto.getIssuePlanId(), orderDto.getWoId(), orderDto.getOutboundRmk(),
                null, null, "SYSTEM", null, null, null, null
        );
        outboundService.updateOutboundOrder(dtoWithId);
        return ApiResponse.ok();
    }

    @PostMapping("/{outboundId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable("outboundId") Long outboundId, @RequestParam("status") String status) {
        outboundService.changeOrderStatus(outboundId, status, "SYSTEM");
        return ApiResponse.ok();
    }

    @DeleteMapping("/{outboundId}")
    public ApiResponse<Void> deleteOutbound(@PathVariable("outboundId") Long outboundId) {
        outboundService.deleteOutboundOrder(outboundId);
        return ApiResponse.ok();
    }
}
