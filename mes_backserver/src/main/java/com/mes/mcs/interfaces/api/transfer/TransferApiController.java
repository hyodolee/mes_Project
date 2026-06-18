package com.mes.mcs.interfaces.api.transfer;

import com.mes.mcs.application.service.transfer.TransferService;
import com.mes.mcs.domain.transfer.dto.TransferItemDto;
import com.mes.mcs.domain.transfer.dto.TransferOrderDto;
import com.mes.mcs.domain.transfer.dto.TransferSearchDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("mcsTransferApiController")
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferApiController {

    private final TransferService transferService;

    @GetMapping
    public ApiResponse<PageResponse<TransferOrderDto>> getTransferList(TransferSearchDto searchDto) {
        return ApiResponse.ok(transferService.getTransferList(searchDto));
    }

    @GetMapping("/{transferId}")
    public ApiResponse<TransferOrderDto> getTransfer(@PathVariable("transferId") Long transferId) {
        return ApiResponse.ok(transferService.getTransferOrder(transferId));
    }

    @GetMapping("/{transferId}/items")
    public ApiResponse<List<TransferItemDto>> getTransferItems(@PathVariable("transferId") Long transferId) {
        return ApiResponse.ok(transferService.getTransferItems(transferId));
    }

    @PostMapping
    public ApiResponse<Long> createTransfer(@RequestBody TransferOrderDto orderDto) {
        String transferNo = orderDto.getTransferNo();
        if (transferNo == null || transferNo.isBlank()) {
            transferNo = "TF-" + System.currentTimeMillis();
        }

        TransferOrderDto dtoWithUser = new TransferOrderDto(
                null, orderDto.getPlantCd(), transferNo, "REQUESTED", orderDto.getFromLocationId(), orderDto.getToLocationId(),
                orderDto.getTransferReason(), "SYSTEM", null, null, null, null, null, null, null, orderDto.getOptimizeRule()
        );
        return ApiResponse.ok(transferService.createTransferOrder(dtoWithUser));
    }

    @PutMapping("/{transferId}")
    public ApiResponse<Void> updateTransfer(@PathVariable("transferId") Long transferId, @RequestBody TransferOrderDto orderDto) {
        TransferOrderDto dtoWithId = new TransferOrderDto(
                transferId, orderDto.getPlantCd(), orderDto.getTransferNo(), orderDto.getTransferStatus(), orderDto.getFromLocationId(), orderDto.getToLocationId(),
                orderDto.getTransferReason(), null, null, "SYSTEM", null, null, null, null, null, orderDto.getOptimizeRule()
        );
        transferService.updateTransferOrder(dtoWithId);
        return ApiResponse.ok();
    }

    @PostMapping("/{transferId}/status")
    public ApiResponse<Void> changeStatus(@PathVariable("transferId") Long transferId, @RequestParam("status") String status) {
        transferService.changeOrderStatus(transferId, status, "SYSTEM");
        return ApiResponse.ok();
    }

    @PostMapping("/{transferId}/items")
    public ApiResponse<Void> createTransferItem(@PathVariable("transferId") Long transferId, @RequestBody TransferItemDto itemDto) {
        transferService.createTransferItem(transferId, itemDto);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{transferId}/items/{transferItemId}")
    public ApiResponse<Void> deleteTransferItem(
            @PathVariable("transferId") Long transferId,
            @PathVariable("transferItemId") Long transferItemId
    ) {
        transferService.deleteTransferItem(transferId, transferItemId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{transferId}")
    public ApiResponse<Void> deleteTransfer(@PathVariable("transferId") Long transferId) {
        transferService.deleteTransferOrder(transferId);
        return ApiResponse.ok();
    }
}
