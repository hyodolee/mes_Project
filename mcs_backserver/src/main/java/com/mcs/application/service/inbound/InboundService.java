package com.mcs.application.service.inbound;

import com.mcs.domain.inbound.dto.InboundItemDto;
import com.mcs.domain.inbound.dto.InboundOrderDto;
import com.mcs.domain.inbound.dto.InboundSearchDto;
import com.mcs.domain.inventory.dto.LocStockDto;
import com.mcs.domain.inventory.dto.LocTransHisDto;
import com.mcs.global.common.dto.PageResponse;
import com.mcs.global.exception.BusinessException;
import com.mcs.global.exception.ErrorCode;
import com.mcs.infra.persistence.mybatis.mapper.inbound.InboundMapper;
import com.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundService {

    private final InboundMapper inboundMapper;
    private final InventoryMapper inventoryMapper;

    public PageResponse<InboundOrderDto> getInboundList(InboundSearchDto searchDto) {
        List<InboundOrderDto> list = inboundMapper.selectInboundList(searchDto);
        long total = inboundMapper.selectInboundCount(searchDto);
        return PageResponse.createPagedResponse(list, total, searchDto);
    }

    public InboundOrderDto getInboundOrder(Long inboundId) {
        return inboundMapper.selectInboundById(inboundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT));
    }

    public List<InboundItemDto> getInboundItems(Long inboundId) {
        return inboundMapper.selectInboundItems(inboundId);
    }

    @Transactional
    public Long createInboundOrder(InboundOrderDto orderDto) {
        inboundMapper.insertInboundOrder(orderDto);
        return orderDto.inboundId();
    }

    @Transactional
    public void updateInboundOrder(InboundOrderDto orderDto) {
        InboundOrderDto existing = getInboundOrder(orderDto.inboundId());
        if (!"PLANNED".equals(existing.inboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.updateInboundOrder(orderDto);
    }

    @Transactional
    public void deleteInboundOrder(Long inboundId) {
        InboundOrderDto existing = getInboundOrder(inboundId);
        if (!"PLANNED".equals(existing.inboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.deleteInboundItems(inboundId);
        inboundMapper.deleteInboundOrder(inboundId);
    }

    @Transactional
    public void addInboundItem(InboundItemDto itemDto) {
        InboundOrderDto order = getInboundOrder(itemDto.inboundId());
        if (!"PLANNED".equals(order.inboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.insertInboundItem(itemDto);
    }

    @Transactional
    public void deleteInboundItem(Long inboundId, Long inboundItemId) {
        InboundOrderDto order = getInboundOrder(inboundId);
        if (!"PLANNED".equals(order.inboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.deleteInboundItem(inboundItemId, inboundId);
    }

    @Transactional
    public void changeOrderStatus(Long inboundId, String newStatus, String userId) {
        InboundOrderDto order = getInboundOrder(inboundId);
        String currentStatus = order.inboundStatus();

        if ("COMPLETED".equals(newStatus) && !"COMPLETED".equals(currentStatus)) {
            List<InboundItemDto> items = getInboundItems(inboundId);
            if (items.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "입고 품목을 먼저 추가해야 합니다.");
            }
            for (InboundItemDto item : items) {
                receiveItem(order, item, userId);
            }
        }

        inboundMapper.updateInboundStatus(inboundId, newStatus, userId);
    }

    private void receiveItem(InboundOrderDto order, InboundItemDto item, String userId) {
        if (item.locationId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "입고 적치 로케이션을 선택해야 합니다.");
        }

        double qty = resolveInboundQty(item);
        String lotNo = normalizeLotNo(item.lotNo());
        LocStockDto stock = getOrCreateLocationStock(order.plantCd(), item.locationId(), item.itemCd(), lotNo, userId);
        double beforeQty = stock.stockQty();
        double afterQty = beforeQty + qty;

        inventoryMapper.updateLocStockQty(stock.locStockId(), qty, userId);
        inventoryMapper.syncLocationUsage(item.locationId(), userId);
        inventoryMapper.insertLocTransHis(new LocTransHisDto(
                null,
                order.plantCd(),
                stock.locStockId(),
                "IB_IN",
                qty,
                beforeQty,
                afterQty,
                "IB",
                order.inboundNo(),
                order.inboundId(),
                order.inboundRmk(),
                userId,
                null,
                null, null, null, null, null, null, null, null
        ));
        inboundMapper.updateInboundItemStatus(item.inboundItemId(), "STOCKED", qty, item.locationId(), userId);
    }

    private LocStockDto getOrCreateLocationStock(String plantCd, Long locationId, String itemCd, String lotNo, String userId) {
        return inventoryMapper.selectLocStockForUpdate(plantCd, locationId, itemCd, lotNo)
                .orElseGet(() -> {
                    inventoryMapper.insertLocStock(new LocStockDto(
                            null,
                            plantCd,
                            locationId,
                            itemCd,
                            lotNo,
                            0.0,
                            0.0,
                            null,
                            userId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ));
                    return inventoryMapper.selectLocStockForUpdate(plantCd, locationId, itemCd, lotNo)
                            .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
                });
    }

    private double resolveInboundQty(InboundItemDto item) {
        Double qty = item.actualQty();
        if (qty == null || qty <= 0) {
            qty = item.expectedQty();
        }
        if (qty == null || qty <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "입고 수량은 0보다 커야 합니다.");
        }
        return qty;
    }

    private String normalizeLotNo(String lotNo) {
        return lotNo == null ? "" : lotNo.trim();
    }
}
