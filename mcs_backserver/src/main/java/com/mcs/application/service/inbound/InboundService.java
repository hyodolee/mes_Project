package com.mcs.application.service.inbound;

import com.mcs.domain.inbound.dto.InboundItemDto;
import com.mcs.domain.inbound.dto.InboundOrderDto;
import com.mcs.domain.inbound.dto.InboundSearchDto;
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
    private final InventoryMapper inventoryMapper; // 입고 확정 시 재고 연동을 위해 사용

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
            throw new BusinessException(ErrorCode.INVALID_INPUT); // "계획" 상태에서만 정보 수정 가능
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
        // TODO: 단건 삭제 쿼리 추가 필요
        // inboundMapper.deleteInboundItemById(inboundItemId);
    }

    @Transactional
    public void changeOrderStatus(Long inboundId, String newStatus, String userId) {
        InboundOrderDto order = getInboundOrder(inboundId);
        String currentStatus = order.inboundStatus();

        // 상태 전이 제약조건 검증 로직 생략 (실무에서는 철저한 검증 필요)
        if ("COMPLETED".equals(newStatus) && !"COMPLETED".equals(currentStatus)) {
            // 입고 확정 처리 로직: 아이템 재고 증가
            List<InboundItemDto> items = getInboundItems(inboundId);
            for (InboundItemDto item : items) {
                if (item.actualQty() != null && item.actualQty() > 0 && item.locationId() != null) {
                    
                    // 주의: 실제로는 해당 위치(Location)+품목(Item)+로트(Lot)를 가진 
                    // MCS_LOCATION_STOCK이 있는지 확인 후 없으면 INSERT, 있으면 UPDATE 해야 함.
                    // 현재는 Phase 3의 기초 뼈대로, 실제 프로젝트에서는 InventoryMapper에 해당 기능 구현이 추가로 필요합니다.

                    // 품목 상태 완료 처리
                    inboundMapper.updateInboundItemStatus(item.inboundItemId(), "STOCKED", item.actualQty(), item.locationId(), userId);
                }
            }
        }
        
        inboundMapper.updateInboundStatus(inboundId, newStatus, userId);
    }
}
