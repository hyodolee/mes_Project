package com.mcs.application.service.outbound;

import com.mcs.domain.inventory.dto.LocStockDto;
import com.mcs.domain.inventory.dto.LocTransHisDto;
import com.mcs.domain.outbound.dto.OutboundItemDto;
import com.mcs.domain.outbound.dto.OutboundOrderDto;
import com.mcs.domain.outbound.dto.OutboundSearchDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import com.mcs.infra.persistence.mybatis.mapper.outbound.OutboundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboundService {

    private final OutboundMapper outboundMapper;
    private final InventoryMapper inventoryMapper;

    public PageResponse<OutboundOrderDto> getOutboundList(OutboundSearchDto searchDto) {
        List<OutboundOrderDto> list = outboundMapper.selectOutboundList(searchDto);
        long total = outboundMapper.selectOutboundCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public OutboundOrderDto getOutboundOrder(Long outboundId) {
        return outboundMapper.selectOutboundById(outboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    public List<OutboundItemDto> getOutboundItems(Long outboundId) {
        return outboundMapper.selectOutboundItems(outboundId);
    }

    @Transactional
    public Long createOutboundOrder(OutboundOrderDto orderDto) {
        outboundMapper.insertOutboundOrder(orderDto);
        return orderDto.outboundId();
    }

    @Transactional
    public void updateOutboundOrder(OutboundOrderDto orderDto) {
        OutboundOrderDto existing = getOutboundOrder(orderDto.outboundId());
        if (!"REQUESTED".equals(existing.outboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        outboundMapper.updateOutboundOrder(orderDto);
    }

    @Transactional
    public void deleteOutboundOrder(Long outboundId) {
        OutboundOrderDto existing = getOutboundOrder(outboundId);
        if (!"REQUESTED".equals(existing.outboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        outboundMapper.deleteOutboundItems(outboundId);
        outboundMapper.deleteOutboundOrder(outboundId);
    }

    @Transactional
    public void changeOrderStatus(Long outboundId, String newStatus, String userId) {
        OutboundOrderDto order = getOutboundOrder(outboundId);
        String currentStatus = order.outboundStatus();

        if ("SHIPPED".equals(newStatus) && !"SHIPPED".equals(currentStatus)) {
            List<OutboundItemDto> items = getOutboundItems(outboundId);
            if (items.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "출고 품목을 먼저 추가해야 합니다.");
            }
            for (OutboundItemDto item : items) {
                shipItem(order, item, userId);
            }
        }

        outboundMapper.updateOutboundStatus(outboundId, newStatus, userId);
    }

    private void shipItem(OutboundOrderDto order, OutboundItemDto item, String userId) {
        if (item.locationId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "출고 로케이션을 선택해야 합니다.");
        }

        double qty = resolveOutboundQty(item);
        String lotNo = normalizeLotNo(item.lotNo());
        LocStockDto stock = inventoryMapper.selectLocStockForUpdate(order.plantCd(), item.locationId(), item.itemCd(), lotNo)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "출고 로케이션의 해당 품목/LOT 재고가 없습니다."
                ));

        if (stock.availableQty() < qty) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "출고 로케이션의 가용 재고가 부족합니다. 현재 가용수량: " + stock.availableQty()
            );
        }

        double beforeQty = stock.stockQty();
        double afterQty = beforeQty - qty;
        inventoryMapper.updateLocStockQty(stock.locStockId(), -qty, userId);
        inventoryMapper.syncLocationUsage(item.locationId(), userId);
        inventoryMapper.insertLocTransHis(new LocTransHisDto(
                null,
                order.plantCd(),
                stock.locStockId(),
                "OB_OUT",
                qty,
                beforeQty,
                afterQty,
                "OB",
                order.outboundNo(),
                order.outboundId(),
                order.outboundRmk(),
                userId,
                null,
                null, null, null, null, null, null, null, null
        ));
        outboundMapper.updateOutboundItemStatus(item.outboundItemId(), "SHIPPED", qty, userId);
    }

    private double resolveOutboundQty(OutboundItemDto item) {
        Double qty = item.shippedQty();
        if (qty == null || qty <= 0) {
            qty = item.pickedQty();
        }
        if (qty == null || qty <= 0) {
            qty = item.allocatedQty();
        }
        if (qty == null || qty <= 0) {
            qty = item.requestedQty();
        }
        if (qty == null || qty <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "출고 수량은 0보다 커야 합니다.");
        }
        return qty;
    }

    private String normalizeLotNo(String lotNo) {
        return lotNo == null ? "" : lotNo.trim();
    }
}
