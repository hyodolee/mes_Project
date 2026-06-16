package com.mes.mcs.application.service.inbound;

import com.mes.mcs.domain.inbound.dto.InboundItemDto;
import com.mes.mcs.domain.inbound.dto.InboundOrderDto;
import com.mes.mcs.domain.inbound.dto.InboundSearchDto;
import com.mes.mcs.domain.inventory.dto.LocStockDto;
import com.mes.mcs.domain.inventory.dto.LocTransHisDto;
import com.mes.global.common.dto.PageResponse;
import com.mes.global.exception.BusinessException;
import com.mes.global.exception.ErrorCode;
import com.mes.mcs.infra.persistence.mybatis.mapper.inbound.InboundMapper;
import com.mes.mcs.infra.persistence.mybatis.mapper.inventory.InventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("mcsInboundService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboundService {

    private final InboundMapper inboundMapper;
    private final InventoryMapper inventoryMapper;

    public PageResponse<InboundOrderDto> getInboundList(InboundSearchDto searchDto) {
        List<InboundOrderDto> list = inboundMapper.selectInboundList(searchDto);
        long total = inboundMapper.selectInboundCount(searchDto);
        return PageResponse.createPagedResponse(list, Math.toIntExact(total), searchDto);
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
        return orderDto.getInboundId();
    }

    @Transactional
    public void updateInboundOrder(InboundOrderDto orderDto) {
        InboundOrderDto existing = getInboundOrder(orderDto.getInboundId());
        if (!"PLANNED".equals(existing.getInboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.updateInboundOrder(orderDto);
    }

    @Transactional
    public void deleteInboundOrder(Long inboundId) {
        InboundOrderDto existing = getInboundOrder(inboundId);
        if (!"PLANNED".equals(existing.getInboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.deleteInboundItems(inboundId);
        inboundMapper.deleteInboundOrder(inboundId);
    }

    @Transactional
    public void addInboundItem(InboundItemDto itemDto) {
        InboundOrderDto order = getInboundOrder(itemDto.getInboundId());
        if (!"PLANNED".equals(order.getInboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.insertInboundItem(itemDto);
    }

    @Transactional
    public void deleteInboundItem(Long inboundId, Long inboundItemId) {
        InboundOrderDto order = getInboundOrder(inboundId);
        if (!"PLANNED".equals(order.getInboundStatus())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        inboundMapper.deleteInboundItem(inboundItemId, inboundId);
    }

    @Transactional
    public void changeOrderStatus(Long inboundId, String newStatus, String userId) {
        InboundOrderDto order = getInboundOrder(inboundId);
        String currentStatus = order.getInboundStatus();

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
        if (item.getLocationId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "입고 적치 로케이션을 선택해야 합니다.");
        }

        double qty = resolveInboundQty(item);
        String lotNo = normalizeLotNo(item.getLotNo());
        LocStockDto stock = getOrCreateLocationStock(order.getPlantCd(), item.getLocationId(), item.getItemCd(), lotNo, userId);
        double beforeQty = stock.getStockQty();
        double afterQty = beforeQty + qty;

        inventoryMapper.updateLocStockQty(stock.getLocStockId(), qty, userId);
        inventoryMapper.syncLocationUsage(item.getLocationId(), userId);
        inventoryMapper.insertLocTransHis(new LocTransHisDto(
                null,
                order.getPlantCd(),
                stock.getLocStockId(),
                "IB_IN",
                qty,
                beforeQty,
                afterQty,
                "IB",
                order.getInboundNo(),
                order.getInboundId(),
                order.getInboundRmk(),
                userId,
                null,
                null, null, null, null, null, null, null, null
        ));
        inboundMapper.updateInboundItemStatus(item.getInboundItemId(), "STOCKED", qty, item.getLocationId(), userId);
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
        Double qty = item.getActualQty();
        if (qty == null || qty <= 0) {
            qty = item.getExpectedQty();
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
